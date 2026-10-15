# TC-A1a-empty：传空数组 → 与未设置等价（不获得"自动探测"含义）

- 规则：A-R1（表 A · build 期决策点）
- 裁决：D7（空数组归一化为 null）
- Note: 这是 ch03 用例 TC-A1a 的第 2 个子变体（传空数组）。陷阱提示：setHeader() 的空数组含义是"从文件首行自动解析表头"（CSVFormat.getHeader() javadoc），但必需表头的名单是断言不是描述，不存在"从文件推断"的语义，所以空数组必须等价于"未设置"。

## 配置

- 表头: (none)
- 必需表头: (empty)

## CSV 数据

(none)

## 预期结果

- 结果: build 通过
- 必需表头返回值: (null)
