# TC-B2b：DISALLOW 模式下重复列名 + 缺必需列 → 重复名错误先抛

- 规则：B-R2（表 B · parse 期决策点）
- 裁决：D3（结构错误优先）

## 配置

- 表头: (auto)
- 必需表头: amount, currency
- 重复表头模式: DISALLOW

## CSV 数据

```csv
date,date,currency
1,2,USD
```

## 预期结果

- 结果: parse 失败
- 异常类型: java.lang.IllegalArgumentException
- 消息包含: The header contains a duplicate name
