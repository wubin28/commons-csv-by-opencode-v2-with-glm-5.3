# TC-B4：仅大小写差异且不忽略大小写 → 缺列失败，缺失名单列出未命中项

- 规则：B-R4（表 B · parse 期决策点）
- 裁决：D2（匹配语义跟随 ignoreHeaderCase，默认 false 即大小写敏感）
- Note: currency 精确命中，date/amount 因大小写不同而未命中。

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

```csv
Date,AMOUNT,currency
1,2,USD
```

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [date, amount]
- 消息包含: in [Date, AMOUNT, currency]
