# TC-B6：仅前后空白差异 → 不命中（不做 trim），消息可见带空格的实际表头

- 规则：B-R6（表 B · parse 期决策点）
- 裁决：D2（不引入第二套比较逻辑：不 trim、不忽略空白）
- Note: 下方数据第三列是"空格+currency+空格"（行尾空格在渲染中不可见，请以源文件字节为准）。默认 trim=false 时表头 key 保留前后空白。

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

```csv
date,amount, currency 
1,2,USD
```

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [currency]
- 消息包含: in [date, amount,  currency ]
