# TC-C1：携带必需列的配置用于打印 → 完全不校验，表头照常输出

- 规则：C-R1（表 C · print 期决策点）
- 裁决：D4（写方向不消费必需列：表头是输出物不是对输入的断言）
- Note: 打印数据会被作为一条数据记录输出；表头行来自配置的手工表头。

## 配置

- 表头: date, amount, currency
- 必需表头: date, amount, currency

## CSV 数据

(none)

## 预期结果

- 结果: print 成功
- 打印数据: 1,2,USD
- 输出行: date,amount,currency
- 输出行: 1,2,USD
