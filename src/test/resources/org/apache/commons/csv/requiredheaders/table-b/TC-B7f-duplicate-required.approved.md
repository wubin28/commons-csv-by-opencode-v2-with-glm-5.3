# TC-B7f：必需列名单自身重复 → 不抛配置错，报缺失时去重保序

- 规则：B-R7（表 B · parse 期决策点）
- 裁决：D7（名单重复不特判；缺失名单去重且保持声明序）

## 配置

- 表头: (auto)
- 必需表头: currency, currency

## CSV 数据

```csv
date,amount
1,2
```

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [currency] in [date, amount]
