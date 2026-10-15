/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.csv;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One approved scenario for the {@code setRequiredHeaders} acceptance suite (Approved Scenarios pattern).
 *
 * <p>
 * A scenario is parsed from a human-reviewable markdown fixture; the fixture is both the review artifact and the only input this test executes.
 * This class has two independent halves:
 * </p>
 * <ul>
 * <li>{@link #parse(Path)}: markdown structure to scenario object. Knows nothing about the CSV API.</li>
 * <li>{@link #execute()}: scenario object to real API calls and assertions. Knows nothing about markdown.</li>
 * </ul>
 *
 * <p>
 * Fixture DSL (Chinese keys for reviewer readability; bracketed English placeholders):
 * </p>
 * <ul>
 * <li>Placeholders: {@code (none)} = setter not called; {@code (null)} = null argument; {@code (empty)} = empty array / zero-byte input;
 * {@code (auto)} = {@code setHeader()} with no arguments (header from the file's first record).</li>
 * <li>Config keys: 表头 / 必需表头 / 忽略表头大小写 / 允许缺失列名 / 重复表头模式 / 跳过表头行. Keys absent from the fixture keep source-code defaults.</li>
 * <li>Outcome keys: 结果 (build 通过 / build 失败 / parse 成功 / parse 失败 / print 成功), plus per-outcome assertions: 异常类型, 消息包含
 * (repeatable), 必需表头返回值, 表头映射键 (ordered), 首条记录 (repeatable {@code name=value}), 打印数据, 输出行 (repeatable, ordered).</li>
 * </ul>
 */
final class RequiredHeadersScenario {

    private static final String NONE = "(none)";
    private static final String NULL = "(null)";
    private static final String EMPTY = "(empty)";
    private static final String AUTO = "(auto)";

    private final String caseId;
    private final String title;
    private final Map<String, String> config = new LinkedHashMap<>();
    private final Map<String, List<String>> expectations = new LinkedHashMap<>();
    /** null = no input for this scenario; "" = zero-byte input; otherwise the exact CSV text. */
    private String csvData;

    private RequiredHeadersScenario(final String caseId, final String title) {
        this.caseId = caseId;
        this.title = title;
    }

    String displayName() {
        return caseId + " · " + title;
    }

    // ------------------------------------------------------------------
    // Parsing half: markdown structure only, no CSV API usage.
    // ------------------------------------------------------------------

    static RequiredHeadersScenario parse(final Path path) throws IOException {
        final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        // First level-1 heading: "# TC-B7a：缺 1 列 ..."
        final String heading = lines.stream().filter(l -> l.startsWith("# ")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Fixture is missing its '# <id>：<title>' heading: " + path));
        final int sep = heading.indexOf('：') >= 0 ? heading.indexOf('：') : heading.indexOf(':');
        final RequiredHeadersScenario scenario = new RequiredHeadersScenario(heading.substring(2, sep).trim(), heading.substring(sep + 1).trim());
        String section = null;
        boolean inFence = false;
        final List<String> fenced = new ArrayList<>();
        for (final String line : lines) {
            if (line.startsWith("## ")) {
                section = line.substring(3).trim();
                continue;
            }
            if (line.startsWith("```")) {
                if (inFence) {
                    if ("CSV 数据".equals(section)) {
                        scenario.csvData = String.join("\n", fenced);
                    }
                    fenced.clear();
                }
                inFence = !inFence;
                continue;
            }
            if (inFence) {
                fenced.add(line); // verbatim: leading/trailing spaces are significant (see TC-B6)
                continue;
            }
            if (!line.startsWith("- ") || section == null) {
                continue;
            }
            final String bullet = line.substring(2);
            final int colon = indexOfKeySeparator(bullet);
            if (colon < 0) {
                continue; // free-form notes: "- Note: ..."
            }
            final String key = bullet.substring(0, colon).trim();
            final String value = bullet.substring(colon + 1).trim();
            if ("配置".equals(section)) {
                scenario.config.put(key, value);
            } else if ("预期结果".equals(section)) {
                scenario.expectations.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            } else if ("CSV 数据".equals(section) && "占位".equals(key)) {
                scenario.csvData = placeholderToCsv(value);
            } else if ("CSV 数据".equals(section) && !NULL.equals(value) && !NONE.equals(value) && !EMPTY.equals(value) && !AUTO.equals(value)) {
                scenario.csvData = placeholderToCsv(value);
            }
        }
        return scenario;
    }

    private static int indexOfKeySeparator(final String bullet) {
        final int ascii = bullet.indexOf(':');
        final int cjk = bullet.indexOf('：');
        if (ascii < 0) {
            return cjk;
        }
        if (cjk < 0) {
            return ascii;
        }
        return Math.min(ascii, cjk);
    }

    private static String placeholderToCsv(final String placeholder) {
        return EMPTY.equals(placeholder) ? "" : null;
    }

    // ------------------------------------------------------------------
    // Execution half: real API calls and assertions, no markdown knowledge.
    // ------------------------------------------------------------------

    void execute() throws Exception {
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder();
        applyConfig(builder);
        switch (single("结果")) {
        case "build 通过":
            assertRequiredHeadersGetter(builder.get());
            break;
        case "build 失败":
            assertFragments(assertThrows(exceptionType(), builder::get));
            break;
        case "parse 成功":
            try (CSVParser parser = builder.get().parse(new StringReader(csvInput()))) {
                assertHeaderMapKeys(parser);
                assertFirstRecord(parser);
            }
            break;
        case "parse 失败":
            final CSVFormat format = builder.get();
            assertFragments(assertThrows(exceptionType(), () -> format.parse(new StringReader(csvInput()))));
            break;
        case "print 成功":
            final StringWriter out = new StringWriter();
            try (CSVPrinter printer = builder.get().print(out)) {
                printer.printRecord((Object[]) splitValues(single("打印数据")));
            }
            assertEquals(expectations.getOrDefault("输出行", new ArrayList<>()), outputLines(out));
            break;
        default:
            throw new IllegalStateException("未知的结果取值: " + single("结果"));
        }
    }

    private void applyConfig(final CSVFormat.Builder builder) {
        final String header = config.get("表头");
        if (AUTO.equals(header)) {
            builder.setHeader();
        } else if (header != null && !NONE.equals(header)) {
            builder.setHeader(splitValues(header));
        }
        final String required = config.get("必需表头");
        if (NULL.equals(required)) {
            builder.setRequiredHeaders((String[]) null);
        } else if (EMPTY.equals(required)) {
            builder.setRequiredHeaders();
        } else if (required != null && !NONE.equals(required)) {
            builder.setRequiredHeaders(splitValues(required));
        }
        setFlag(builder, "忽略表头大小写", builder::setIgnoreHeaderCase);
        setFlag(builder, "允许缺失列名", builder::setAllowMissingColumnNames);
        setFlag(builder, "跳过表头行", builder::setSkipHeaderRecord);
        final String duplicateMode = config.get("重复表头模式");
        if (duplicateMode != null) {
            builder.setDuplicateHeaderMode(DuplicateHeaderMode.valueOf(duplicateMode));
        }
    }

    private void setFlag(final CSVFormat.Builder builder, final String key, final java.util.function.Consumer<Boolean> setter) {
        final String value = config.get(key);
        if (value != null) {
            setter.accept(Boolean.valueOf(value));
        }
    }

    private Class<? extends Throwable> exceptionType() throws ClassNotFoundException {
        return Class.forName(single("异常类型")).asSubclass(Throwable.class);
    }

    private void assertRequiredHeadersGetter(final CSVFormat format) {
        final String expected = expectations.containsKey("必需表头返回值") ? single("必需表头返回值") : null;
        if (expected == null || NULL.equals(expected)) {
            assertNull(format.getRequiredHeaders(), caseId + ": getRequiredHeaders() 应为 null");
        } else {
            assertArrayEquals(splitValues(expected), format.getRequiredHeaders(), caseId + ": getRequiredHeaders() 名单与顺序");
        }
    }

    private void assertHeaderMapKeys(final CSVParser parser) {
        if (expectations.containsKey("表头映射键")) {
            assertEquals(Arrays.asList(splitValues(single("表头映射键"))), new ArrayList<>(parser.getHeaderMap().keySet()),
                    caseId + ": headerMap 键集合（按迭代序）");
        }
    }

    private void assertFirstRecord(final CSVParser parser) {
        final List<String> expected = expectations.get("首条记录");
        if (expected == null || expected.isEmpty()) {
            return;
        }
        final CSVRecord record = parser.iterator().next();
        assertNotNull(record, caseId + ": 应至少有一条数据记录");
        for (final String pair : expected) {
            final int eq = pair.indexOf('=');
            assertEquals(pair.substring(eq + 1), record.get(pair.substring(0, eq)), caseId + ": 首条记录的 " + pair.substring(0, eq));
        }
    }

    private void assertFragments(final Throwable thrown) {
        assertNotNull(thrown.getMessage(), caseId + ": 异常消息不应为 null");
        for (final String fragment : expectations.getOrDefault("消息包含", new ArrayList<>())) {
            assertTrue(thrown.getMessage().contains(fragment),
                    caseId + ": 消息应包含 <" + fragment + ">，实际是 <" + thrown.getMessage() + ">");
        }
    }

    private static List<String> outputLines(final StringWriter out) {
        final List<String> lines = new ArrayList<>();
        for (final String line : out.toString().split("\\r\\n|\\n|\\r")) {
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private String csvInput() {
        return csvData == null ? "" : csvData;
    }

    private String single(final String key) {
        final List<String> values = expectations.get(key);
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException(caseId + ": fixture 缺少必需的预期键 <" + key + ">");
        }
        return values.get(0);
    }

    private static String[] splitValues(final String value) {
        final List<String> parts = new ArrayList<>();
        for (final String part : value.split(",")) {
            final String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts.toArray(new String[0]);
    }
}
