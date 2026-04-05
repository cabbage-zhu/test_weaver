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

## 测试结构
- `@BeforeMethod` 做初始化，`@AfterMethod` 做清理
- 使用 `MockitoAnnotations.openMocks(this)` 初始化 mock
- 使用 `@DataProvider` 做参数化测试
- 每个测试方法聚焦一个场景，注释说明覆盖的分支路径
