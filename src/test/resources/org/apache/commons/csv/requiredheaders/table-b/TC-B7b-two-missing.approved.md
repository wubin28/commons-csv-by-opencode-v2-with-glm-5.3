# TC-B7b：缺 2 列 → 缺失名单按声明顺序排列

- 规则：B-R7（表 B · parse 期决策点）
- 裁决：D1（消息确定性：按 setRequiredHeaders 的声明序）

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

```csv
date
1
```

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [amount, currency] in [date]
