# TC-B7c：缺全部列 → 缺失名单列全部必需列

- 规则：B-R7（表 B · parse 期决策点）
- 裁决：D1

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

```csv
foo,bar
1,2
```

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [date, amount, currency] in [foo, bar]
