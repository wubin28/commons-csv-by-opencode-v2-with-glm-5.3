# TC-B3：表头完整含全部必需列 → 解析成功、按名访问可用

- 规则：B-R3（表 B · parse 期决策点）
- 裁决：D2（命中判定复用 headerMap）

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

```csv
date,amount,currency
1,2,USD
```

## 预期结果

- 结果: parse 成功
- 表头映射键: date, amount, currency
- 首条记录: currency=USD
