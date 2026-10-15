# TC-A3b：手工指定表头 + 必需列 → build 同样通过（单点校验）

- 规则：A-R3（表 A · build 期决策点）
- 裁决：D5（拒绝双时机：手工模式也不在 validate() 做必需列交叉检查，避免与 parse 期大小写语义分叉）

## 配置

- 表头: date, amount, currency
- 必需表头: date, amount

## CSV 数据

(none)

## 预期结果

- 结果: build 通过
- 必需表头返回值: date, amount
