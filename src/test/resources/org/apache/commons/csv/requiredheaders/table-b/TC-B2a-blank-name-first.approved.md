# TC-B2a：既有空列名结构错误 + 缺必需列 → 空名错误先抛（非 CSVException）

- 规则：B-R2（表 B · parse 期决策点）
- 裁决：D3（结构性错误绝对优先于必需列校验）
- Note: 允许缺失列名默认 false（源码 Builder 字段无初始化器，与 ch03 文档一致）。表头行第二列是空串。

## 配置

- 表头: (auto)
- 必需表头: currency

## CSV 数据

```csv
date,,currency
1,2,USD
```

## 预期结果

- 结果: parse 失败
- 异常类型: java.lang.IllegalArgumentException
- 消息包含: A header name is missing in
