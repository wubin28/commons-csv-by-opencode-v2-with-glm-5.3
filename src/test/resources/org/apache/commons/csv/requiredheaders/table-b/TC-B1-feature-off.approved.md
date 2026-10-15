# TC-B1：未设置必需列 → 解析行为与现状完全一致（特性默认关闭）

- 规则：B-R1（表 B · parse 期决策点）
- 裁决：D7（未设置即关闭）

## 配置

- 表头: (auto)
- 必需表头: (none)

## CSV 数据

```csv
date,amount,currency
1,2,USD
```

## 预期结果

- 结果: parse 成功
- 首条记录: date=1
- 首条记录: currency=USD
