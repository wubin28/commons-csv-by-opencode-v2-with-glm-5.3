# TC-B3a：手工表头 + skip → 校验行为与自动探测一致（对照手工名单）

- 规则：B-R3（表 B · parse 期决策点）
- 裁决：D5、D9（单点校验使两模式行为一致；对照手工名单而非文件首行）
- Note: 已显式设置"跳过表头行"，所以数据区可以含一行表头样内容（它会被 skip 掉）——这是"显式指定表头时数据不重复表头行"约定的例外情形。

## 配置

- 表头: date, amount, currency
- 必需表头: date, amount, currency
- 跳过表头行: true

## CSV 数据

```csv
DATE,AMT,CUR
1,2,USD
```

## 预期结果

- 结果: parse 成功
- 表头映射键: date, amount, currency
- 首条记录: currency=USD
