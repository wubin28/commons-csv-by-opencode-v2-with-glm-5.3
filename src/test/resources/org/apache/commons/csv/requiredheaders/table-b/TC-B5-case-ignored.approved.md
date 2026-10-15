# TC-B5：仅大小写差异且忽略大小写 → 命中通过，大小写两种名字都取到值

- 规则：B-R5（表 B · parse 期决策点）
- 裁决：D2（复用同一张 CASE_INSENSITIVE 映射表）
- Note: ignoreHeaderCase=true 时映射表是 TreeMap(CASE_INSENSITIVE_ORDER)，键按"忽略大小写的字母序"迭代：AMOUNT, currency, Date——与插入顺序不同，这是 TreeMap 的固有行为。

## 配置

- 表头: (auto)
- 必需表头: date, amount, currency
- 忽略表头大小写: true

## CSV 数据

```csv
Date,AMOUNT,currency
1,2,USD
```

## 预期结果

- 结果: parse 成功
- 表头映射键: AMOUNT, currency, Date
- 首条记录: amount=2
- 首条记录: AMOUNT=2
- 首条记录: currency=USD
