# TC-B7e：手工表头对照手工名单（文件首行含必需列也不算数）

- 规则：B-R7（表 B · parse 期决策点）
- 裁决：D9（对照对象是手工名单建立的映射，与文件内容无关）
- Note: 手工模式且未设"跳过表头行"，数据区首行本会被当作第一条数据记录读取——但缺列异常在读任何数据之前（构造期）抛出，数据内容对本用例结果无影响；故意让文件首行含 currency 是为了证明"不对照文件"。

## 配置

- 表头: date, amount
- 必需表头: currency

## CSV 数据

```csv
date,amount,currency
1,2,USD
```

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [currency] in [date, amount]
