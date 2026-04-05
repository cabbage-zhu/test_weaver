# 单元测试硬性约束

## 框架选择
- 使用 TestNG，不使用 JUnit
- 使用 Mockito 做 mock，绝不使用 PowerMock、EasyMock
- 静态方法依赖通过 `java.lang.reflect.Field` 反射注入 mock 对象（参考 `examples/testng-reflection.java`）

## 测试文件位置
- 所有生成的单元测试放在 `src/test/java/ut/` 目录下
- 包路径与被测类保持一致，例如被测类 `com.example.service.FooService` 对应测试 `ut.com.example.service.FooServiceTest`
- `src/test/java/integration/` 是集成测试目录，Skill 不生成、不修改其中的文件

## 命名规范
- 测试类名：`{被测类名}Test`，例如 `FooServiceTest`
- 测试方法名：驼峰式 `test{方法名}{测试内容}`，例如 `testCalculateDiscountWhenQuantityOver100`
- 不使用下划线分隔

## 覆盖要求
- 必须覆盖代码路径和分支，不只是行覆盖
- if/else 的两个分支都要覆盖
- switch/case 的每个 case + default 都要覆盖
- try/catch 的正常路径和异常路径都要覆盖
- null 检查的 null 和非 null 路径都要覆盖
- 边界值（0、负数、恰好等于阈值）要覆盖

## 不可测代码处理
- 如果某段代码实在无法通过反射注入的方式测试（如 native 方法、框架生命周期回调），跳过并在报告中说明原因
- 不要为了覆盖率强行写无意义的测试

## 业务代码
- 永远不修改业务代码（`src/main/java` 下的文件）
- 如果发现业务代码存在 bug，在报告中指出，但不直接修改

## 测试结构与优雅实践

### 基础结构
- `@BeforeMethod` 做初始化，`@AfterMethod` 做清理
- 使用 `MockitoAnnotations.openMocks(this)` 初始化 mock
- 每个测试方法聚焦一个场景，注释说明覆盖的分支路径

### 参数化测试（@DataProvider）
- 同一方法的多种输入/分支场景，优先用 `@DataProvider` 参数化覆盖，避免重复测试方法
- 当输入组合 >= 3 种时，必须用 `@DataProvider`
- DataProvider 返回 `Object[][]`，每行是一组参数 + 描述

```java
@DataProvider(name = "discountScenarios")
public Object[][] discountScenarios() {
    return new Object[][]{
        {1500.0, "NORMAL", 1350.0, "大额折扣"},
        {500.0,  "NORMAL", 475.0,  "普通折扣"},
        {1000.0, "NORMAL", 950.0,  "边界值"},
    };
}

@Test(dataProvider = "discountScenarios", description = "参数化验证折扣计算")
public void testCalculateDiscount(double amount, String type, double expected, String desc) {
    assertEquals(service.calculateDiscount(amount, type), expected, 0.01, desc);
}
```

### Mock 行为复用
- 重复的 `when(...).thenReturn(...)` 抽成 private 方法
- 重复的 `verify(...)` 逻辑也抽成方法

```java
private void mockPaymentSuccess() {
    when(paymentGateway.charge(anyDouble())).thenReturn(true);
}

private void verifyOnlyEmailCalled(String address, String message) {
    verify(emailClient).send(address, message);
    verify(smsClient, never()).send(anyString(), anyString());
}
```

### TestNG 注解充分利用
- `@Test(description = "...")` — 每个测试必须写 description
- `@Test(dataProvider = "...")` — 参数化测试
- `@Test(expectedExceptions = XxxException.class)` — 异常验证，不要用 try/catch + fail
- `@Test(expectedExceptionsMessageRegExp = "...")` — 异常消息正则匹配
- `@Test(groups = "...")` — 按分支类型分组（"if-else"、"switch-case"、"exception"、"null-check"）
- `@Test(timeOut = 3000)` — 性能敏感方法加超时保护
- `@Test(dependsOnMethods = "...")` — 有前置依赖的测试声明依赖关系

### 高级断言
- `SoftAssert` — 需要验证多个属性时，批量断言一次收集所有失败

```java
SoftAssert sa = new SoftAssert();
sa.assertEquals(result.getCode(), 200);
sa.assertEquals(result.getMessage(), "OK");
sa.assertTrue(result.isSuccess());
sa.assertAll();
```

- `ArgumentCaptor` — 验证传参细节

```java
ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
verify(repository).save(captor.capture());
assertEquals(captor.getValue().getStatus(), "PAID");
```

- `InOrder` — 验证调用顺序

```java
var inOrder = inOrder(paymentGateway, orderRepository);
inOrder.verify(paymentGateway).charge(100.0);
inOrder.verify(orderRepository).save(order);
```

### 反射注入（替代 PowerMock）
- 私有字段无 setter 时，用反射注入 mock
- 静态字段用 `injectStaticField()` 注入

```java
private void injectField(Object target, String fieldName, Object value) throws Exception {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
}

private void injectStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
}
```
