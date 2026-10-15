# TC-B7d：空文件 + 自动探测 → 全部缺失，实际表头为空列表

- 规则：B-R7（表 B · parse 期决策点）
- 裁决：D8（空文件不特判：headerMap 是空映射而非 null，统一走"缺全部"）
- Note: CSV 数据占位符 (empty) 表示零字节输入。

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency

## CSV 数据

(empty)

## 预期结果

- 结果: parse 失败
- 异常类型: org.apache.commons.csv.CSVException
- 消息包含: Missing required header(s) [date, amount, currency] in []
