# 疑似 Bug 报告模板

当单元测试结果与预期不符，且测试逻辑本身正确时，使用以下格式报告疑似业务代码 bug。

---

## 疑似 Bug #{序号}

**文件**: `com/example/service/FooService.java`
**方法**: `calculateDiscount(double amount, String memberType)`
**行号**: 42

**期望行为**: 当 quantity > 100 时，折扣应为 15%
**实际行为**: 折扣为 10%，因为条件使用了 `>= 100` 而非 `> 100`

**测试证据**: `testCalculateDiscountWhenQuantityOver100` 断言失败
```
Expected: 0.15
Actual:   0.10
```

**严重程度**: 中（业务逻辑）

**分析**: 第 42 行 `if (quantity >= 100)` 应为 `if (quantity > 100)`，导致 quantity 恰好为 100 时错误地应用了高折扣。

> 注意：业务代码未被修改。请开发者人工确认并修复。
