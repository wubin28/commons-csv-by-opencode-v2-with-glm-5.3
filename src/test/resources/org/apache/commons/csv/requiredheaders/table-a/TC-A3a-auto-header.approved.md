# TC-A3a：自动探测表头 + 必需列 → build 通过、校验推迟到 parse 期

- 规则：A-R3（表 A · build 期决策点）
- 裁决：D5（读入期单点校验，不在 build 期提前查）
- Note: getter 返回的名单与设置顺序一致且是拷贝。

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

(none)

## 预期结果

- 结果: build 通过
- 必需表头返回值: date, amount, currency
