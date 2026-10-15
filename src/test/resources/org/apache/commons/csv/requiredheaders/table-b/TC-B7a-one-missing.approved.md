# TC-B7a：缺 1 列（currency 不存在）→ CSVException，消息含缺失名单与实际表头

- 规则：B-R7（表 B · parse 期决策点）
- 裁决：D1（异常类型与消息模板）

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

```csv
date,amount
1,2
```

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [currency] in [date, amount]
