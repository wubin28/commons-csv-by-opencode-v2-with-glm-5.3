# TC-B2c：结构错误 + 必需列全部命中 → 仍抛结构错误（优先级与命中无关）

- 规则：B-R2（表 B · parse 期决策点）
- 裁决：D3（决策表 B-R2 行"差异形态"是 don't care 的实证）

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
