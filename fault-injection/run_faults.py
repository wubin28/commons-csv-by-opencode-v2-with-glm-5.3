#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fault injection closed-loop driver (ch05 方案一).

For each of the 22 recipes in faults.json:
  1. preflight: target production file must be clean in git, and the recipe's
     `original` snippet must appear exactly once in the file (line-number drift
     or refactor => safe abort, never a wrong injection).
  2. inject: exact-text replacement (bytes-safe).
  3. RED: run only the fixture's enclosing @ParameterizedTest method via
     -Dtest=..., parse the surefire XML, and check three things:
       a. the target fixture invocation failed;
       b. its failure message contains every expected fragment (the failure is
          about the protected behaviour, not a random breakage);
       c. the ACTUAL set of red fixtures equals the PREDICTED set (collateral
          proof: the fault's blast radius is the set of fixtures that protect
          the same semantic side).
  4. revert: git checkout -- <file> (in a finally block), then verify the file
     is byte-identical to the pre-injection snapshot.
  5. GREEN: re-run the same method, require failures=0 errors=0.

After all cycles: one full `mvn test` against the regression baseline
(Tests run: 1025, Failures: 0, Errors: 0, Skipped: 11) and a run log written
to the chapter directory (../ch05-4-fault-injection-run.log).

Usage (from the repo root or the fault-injection dir, macOS zsh):
    python3 fault-injection/run_faults.py            # all 22
    python3 fault-injection/run_faults.py TC-B4      # single recipe
"""

import json
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
REPO = SCRIPT_DIR.parent
SUITE = "RequiredHeadersApprovedScenariosTest"
REPORT_XML = REPO / "target" / "surefire-reports" / ("TEST-org.apache.commons.csv.%s.xml" % SUITE)
FIXTURE_ROOT = REPO / "src" / "test" / "resources" / "org" / "apache" / "commons" / "csv" / "requiredheaders"
LOG_PATH = REPO.parent / "ch05-4-fault-injection-run.log"
MVN_TIMEOUT = 900
BASELINE = {"tests": 1025, "failures": 0, "errors": 0, "skipped": 11}


def run_mvn(args, quiet=True):
    cmd = ["mvn"] + (["-q"] if quiet else []) + args
    return subprocess.run(cmd, cwd=REPO, capture_output=True, text=True, timeout=MVN_TIMEOUT)


def fixture_case_ids(table):
    """caseId of every fixture in a table dir, in the harness's own order
    (Files.list().sorted() == sorted filenames; H1 heading gives the caseId,
    exactly like RequiredHeadersScenario.parse does)."""
    ids = []
    for path in sorted((FIXTURE_ROOT / table).glob("*.approved.md")):
        first = next(l for l in path.read_text(encoding="utf-8").split("\n") if l.startswith("# "))
        sep = first.find("：")
        alt = first.find(":")
        if sep < 0 or (alt >= 0 and alt < sep):
            sep = alt
        ids.append(first[2:sep].strip())
    return ids


def parse_report():
    """[(name, failed, message)] in document order == invocation order."""
    root = ET.parse(REPORT_XML).getroot()
    out = []
    for tc in root.iter("testcase"):
        node = tc.find("failure")
        if node is None:
            node = tc.find("error")
        failed = node is not None
        msg = ((node.get("message") or "") + "\n" + (node.text or "")) if failed else ""
        out.append((tc.get("name"), failed, msg))
    return out, root.get("tests"), root.get("failures"), root.get("errors")


def run_suite_method(method):
    """Run one enclosing method; returns (report_rows, suite_attrs, proc, stale)."""
    before = REPORT_XML.stat().st_mtime if REPORT_XML.exists() else 0
    started = time.time()
    proc = run_mvn(["test", "-Dtest=%s#%s" % (SUITE, method), "-DfailIfNoTests=false"])
    elapsed = time.time() - started
    if not REPORT_XML.exists() or REPORT_XML.stat().st_mtime <= before:
        return None, None, proc, True, elapsed
    rows, t, f, e = parse_report()
    return rows, (t, f, e), proc, False, elapsed


def tail(proc, n=25):
    lines = (proc.stdout + "\n" + proc.stderr).strip().split("\n")
    return "\n".join(lines[-n:])


def main():
    only = sys.argv[1] if len(sys.argv) > 1 else None
    recipes = json.loads((SCRIPT_DIR / "faults.json").read_text(encoding="utf-8"))["recipes"]
    if only:
        recipes = [r for r in recipes if r["id"] == only]
        if not recipes:
            sys.exit("no recipe with id %s" % only)

    log_lines = ["# ch05 故障注入闭环运行台账",
                 "",
                 "- 生成时间: %s" % datetime.now().isoformat(timespec="seconds"),
                 "- 仓库: %s @ %s" % (REPO.name, subprocess.run(["git", "-C", str(REPO), "rev-parse", "--short", "HEAD"],
                                                        capture_output=True, text=True).stdout.strip()),
                 "- 循环: 注入 → 方法级运行验证变红（片段 + 连带集合）→ git checkout 撤销 → 再运行验证变绿",
                 ""]
    verdicts = []
    fail_count = 0

    for r in recipes:
        rid, method, table = r["id"], r["method"], r["table"]
        target_file = REPO / r["file"]
        case_ids = fixture_case_ids(table)
        head = "== %s (%s) — %s:%s" % (rid, method, r["file"].rsplit("/", 1)[-1], r["line"])
        log_lines += ["", head, "-" * len(head)]
        print(head)

        def fail(reason, detail=""):
            nonlocal fail_count
            fail_count += 1
            verdicts.append((rid, "FAIL", reason))
            log_lines.append("  ✗ FAIL: %s" % reason)
            if detail:
                log_lines.append("    " + detail.replace("\n", "\n    "))

        # ---- 1. preflight ----
        dirty = subprocess.run(["git", "-C", str(REPO), "status", "--porcelain", "--", r["file"]],
                               capture_output=True, text=True).stdout.strip()
        if dirty:
            fail("preflight: 目标文件在工作区不干净，拒绝注入", dirty)
            continue
        original_bytes = target_file.read_bytes()
        text = original_bytes.decode("utf-8")
        n = text.count(r["original"])
        if n != 1:
            fail("preflight: original 片段出现 %d 次（要求恰好 1 次），配方与源码漂移" % n)
            continue

        try:
            # ---- 2. inject ----
            target_file.write_bytes(text.replace(r["original"], r["faulted"], 1).encode("utf-8"))
            log_lines.append("  注入: %s:%s 片段替换完成" % (r["file"], r["line"]))

            # ---- 3. RED ----
            rows, attrs, proc, stale, elapsed = run_suite_method(method)
            if stale:
                fail("变红阶段: surefire 报告未更新（疑似编译失败）", tail(proc))
                continue
            reds = [case_ids[i] for i, row in enumerate(rows) if row[1]]
            target_idx = case_ids.index(rid)
            name, failed, msg = rows[target_idx]
            if not failed:
                fail("变红阶段: 目标 fixture %s 未失败（该故障未击中保护面）" % rid,
                     "testcase=%s message=%r" % (name, msg[:300]))
            else:
                missing_frags = [f for f in r["expectedFragments"] if f not in msg]
                if missing_frags:
                    fail("变红阶段: 失败信息缺少期望片段 %r" % missing_frags, msg[:600])
                elif set(reds) != set(r["expectedRedSet"]):
                    fail("变红阶段: 实际连带变红集合与推演不符",
                         "预期 %s\n    实际 %s" % (sorted(r["expectedRedSet"]), sorted(reds)))
                else:
                    verdicts.append((rid, "RED-OK", "片段命中；连带集合 = 预期（%d 个 fixture 红）" % len(reds)))
                    log_lines.append("  变红: %s 失败且消息含全部期望片段" % rid)
                    log_lines.append("  连带: 实际变红集合 = 预期 %s" % sorted(reds))
                    log_lines.append("  失败消息（截断）: %s" % msg.strip().split("\n")[0][:220])
            log_lines.append("  运行: %s（%ds，tests=%s failures=%s errors=%s）" %
                             (method, int(elapsed), attrs[0], attrs[1], attrs[2]))
        finally:
            # ---- 4. revert (always) ----
            subprocess.run(["git", "-C", str(REPO), "checkout", "--", r["file"]], check=True)
            restored = target_file.read_bytes() == original_bytes
            log_lines.append("  撤销: git checkout 完成，文件%s恢复到注入前字节状态" % ("" if restored else "【未】"))
            if not restored:
                fail("撤销阶段: 文件未恢复到注入前状态")

        # ---- 5. GREEN ----
        rows, attrs, proc, stale, elapsed = run_suite_method(method)
        if stale:
            fail("变绿阶段: surefire 报告未更新（疑似编译失败）", tail(proc))
            continue
        n_red = sum(1 for row in rows if row[1])
        if proc.returncode == 0 and attrs[1] == "0" and attrs[2] == "0" and n_red == 0:
            verdicts.append((rid, "GREEN-OK", "撤销后 %s 全绿（%d 个 fixture）" % (method, len(rows))))
            log_lines.append("  变绿: %s 全部 %d 个 fixture 通过（%ds）" % (method, len(rows), int(elapsed)))
        else:
            fail("变绿阶段: 撤销后仍有失败", "returncode=%s failures=%s errors=%s red=%d\n%s" %
                 (proc.returncode, attrs[1], attrs[2], n_red, tail(proc, 10)))

    # ---- final full regression ----
    summary = ""
    if not only:
        log_lines += ["", "== 全量回归（mvn test，对照基线 1025/0/0/11）", "-" * 40]
        print("full mvn test ...")
        proc = run_mvn(["test"], quiet=False)
        lines = [l for l in (proc.stdout + "\n" + proc.stderr).split("\n") if "Tests run:" in l]
        total = lines[-1].strip() if lines else "(no summary line)"
        ok = proc.returncode == 0 and "Failures: 0" in total and "Errors: 0" in total
        log_lines.append("  %s → %s" % (total, "PASS（与基线口径一致）" if ok else "FAIL"))
        summary = total
        if not ok:
            fail_count += 1
            verdicts.append(("FULL-SUITE", "FAIL", total))
        else:
            verdicts.append(("FULL-SUITE", "GREEN-OK", total))

    # ---- verdict table + log ----
    log_lines += ["", "== 结算", "-" * 40]
    for rid, kind, note in verdicts:
        log_lines.append("  %-9s %-8s %s" % (rid, kind, note))
    red_ok = sum(1 for _, k, _ in verdicts if k == "RED-OK")
    green_ok = sum(1 for _, k, _ in verdicts if k == "GREEN-OK")
    log_lines.append("")
    log_lines.append("  闭环成功 %d/%d（变红+片段+连带 %d，变绿 %d，失败 %d）" %
                     (red_ok, len(recipes), red_ok, green_ok, fail_count))
    LOG_PATH.write_text("\n".join(log_lines) + "\n", encoding="utf-8")
    print("\n%s\n闭环成功 %d/%d，失败 %d。台账: %s" % ("-" * 60, red_ok, len(recipes), fail_count, LOG_PATH))
    sys.exit(1 if fail_count else 0)


if __name__ == "__main__":
    main()
