# TC-A2：设了必需列却从未设置表头 → build 期抛 IllegalArgumentException

- 规则：A-R2（表 A · build 期决策点）
- 裁决：D6（配置矛盾在 validate() fail-fast）

## 配置

- 表头: (none)
- 必需表头: date, amount, currency

## CSV 数据

(none)

## 预期结果

- 结果: build 失败
- 异常类型: java.lang.IllegalArgumentException
- 消息包含: Required headers [date, amount, currency] are set
- 消息包含: no header is defined
