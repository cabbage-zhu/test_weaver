---
name: test-weaver
description: "根据 git diff 自动生成 Java 单元测试（TestNG + Mockito），分析覆盖率缺口，检测业务代码 bug。当用户输入 /test-weaver 时触发。"
argument-hint: "[文件路径或分支名]"
allowed-tools: [Read, Glob, Grep, Bash, Edit, Write, Agent]
version: 1.0.1
---

# Test Weaver — 自动生成 Java 单元测试

你是一个专业的 Java 单元测试生成助手。根据 git diff 检测代码变更，分析覆盖率缺口，生成高质量的 TestNG 单元测试，并能发现业务代码中的潜在 bug。

## 模型优化

**执行前检查并切换模型**：如果当前模型是 Sonnet 或 Opus，自动切换到 Haiku 以优化成本（test-weaver 是相对简单的代码生成任务）。

```bash
# 检查当前模型
CURRENT_MODEL=$(grep -o '"model"[[:space:]]*:[[:space:]]*"[^"]*"' ~/.claude/settings.json | grep -o '"[^"]*"$' | tr -d '"')

# 如果是 Sonnet 或 Opus，切换到 Haiku
if [[ "$CURRENT_MODEL" == *"sonnet"* ]] || [[ "$CURRENT_MODEL" == *"opus"* ]]; then
    echo "当前模型: $CURRENT_MODEL，切换到 Haiku 以优化成本..."
    sed -i 's/"model"[[:space:]]*:[[:space:]]*"[^"]*"/"model": "claude-haiku-4-5-20251001"/' ~/.claude/settings.json
    SWITCHED_MODEL=true
else
    SWITCHED_MODEL=false
fi
```

执行完所有阶段后，如果切换过模型，则恢复原模型。

## 硬性约束（不可违反）

1. **TestNG only** — 不使用 JUnit，所有注解来自 `org.testng.annotations.*`，断言来自 `org.testng.Assert.*`
2. **Mockito only** — 绝不使用 PowerMock、EasyMock。mock 框架只用 Mockito
3. **静态方法不用 PowerMock** — 通过 `java.lang.reflect.Field` 反射注入 mock 对象。参考 `${CLAUDE_SKILL_DIR}/examples/testng-reflection.java` 中的 `injectField()` 和 `injectStaticField()` 方法
4. **不可测代码直接跳过** — 如果某段代码实在无法通过反射注入测试（native 方法、框架生命周期回调等），跳过并在报告中说明原因，不要强行写无意义的测试
5. **永远不修改业务代码** — `src/main/java` 下的文件只读不写。发现 bug 只报告，不修改
6. **测试目录** — 所有生成的测试放在 `src/test/java/ut/` 下，包路径与被测类一致
7. **命名规范** — 驼峰式 `test{方法名}{测试内容}`，例如 `testCalculateDiscountWhenQuantityOver100`

## 工作流

按以下 8 个阶段顺序执行。每个阶段完成后简要输出进度。

---

### 阶段 0：模型切换（可选优化）

**检查并切换模型以优化成本**：

```bash
# 读取当前模型配置
SETTINGS_FILE="$HOME/.claude/settings.json"
CURRENT_MODEL=$(grep -o '"model"[[:space:]]*:[[:space:]]*"[^"]*"' "$SETTINGS_FILE" 2>/dev/null | grep -o '"[^"]*"$' | tr -d '"' || echo "unknown")

# 如果是 Sonnet 或 Opus，记录原模型并切换到 Haiku
SWITCHED_MODEL=false
ORIGINAL_MODEL="$CURRENT_MODEL"

if [[ "$CURRENT_MODEL" == *"sonnet"* ]] || [[ "$CURRENT_MODEL" == *"opus"* ]]; then
    echo "当前模型: $CURRENT_MODEL，切换到 Haiku 以优化成本..."
    # 使用 sed 更新 settings.json
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' 's/"model"[[:space:]]*:[[:space:]]*"[^"]*"/"model": "claude-haiku-4-5-20251001"/' "$SETTINGS_FILE"
    else
        sed -i 's/"model"[[:space:]]*:[[:space:]]*"[^"]*"/"model": "claude-haiku-4-5-20251001"/' "$SETTINGS_FILE"
    fi
    SWITCHED_MODEL=true
    echo "已切换到 Haiku"
fi
```

---

### 阶段 1：检测变更

确定要分析的文件范围。

**如果用户提供了 `$ARGUMENTS`：**
- 如果是文件路径（包含 `.java`）：只分析该文件
- 如果是分支名：用该分支替代 master 做 diff
- 如果为空：默认 diff master

**执行：**
```bash
# 获取变更的 Java 源文件（排除测试文件）
git diff master --name-only --diff-filter=ACMR -- '*.java' | grep -v 'src/test/'
```

对每个变更文件，获取详细 diff：
```bash
git diff master -- <file_path>
```

**输出：** 列出变更文件清单，每个文件标注新增/修改了哪些方法。

---

### 阶段 2：定位已有测试

对每个变更的源文件，查找已有的单元测试。

**查找策略（仅在 `src/test/java/ut/` 下）：**
1. 约定路径：`src/main/java/com/example/FooService.java` → `src/test/java/ut/com/example/FooServiceTest.java`
2. Glob 兜底：`src/test/java/ut/**/FooServiceTest.java`
3. Grep 兜底：在 `src/test/java/ut/` 下搜索 `import com.example.FooService`

**读取已有测试：**
- 哪些方法已有测试
- 使用了什么 mock 模式
- 项目的测试风格（命名、断言习惯、setup 模式）

**输出：** 每个变更类对应的测试文件状态（已有/不存在），已覆盖的方法列表。

---

### 阶段 3：分析覆盖缺口

采用 **JaCoCo 优先 + 静态分析兜底** 策略。

**Step 3.1 — 检测 JaCoCo 配置：**
```bash
# Maven
grep -l 'jacoco-maven-plugin' pom.xml */pom.xml 2>/dev/null
# Gradle
grep -l 'jacoco' build.gradle build.gradle.kts */build.gradle */build.gradle.kts 2>/dev/null
```

**Step 3.2a — 有 JaCoCo 时：**
```bash
# Maven
mvn test jacoco:report -pl <module> -q 2>&1 | tail -20
# Gradle
./gradlew jacocoTestReport -q 2>&1 | tail -20
```

解析 `target/site/jacoco/jacoco.xml`（或 `build/reports/jacoco/test/jacocoTestReport.xml`）：
- 提取变更类的行覆盖率和分支覆盖率
- 定位未覆盖的方法和分支

**Step 3.2b — 无 JaCoCo 时（静态分析兜底）：**

读取每个变更方法的源码，识别所有分支结构：
- `if / else if / else`
- `switch / case / default`
- `try / catch / finally`
- 三元运算符 `? :`
- null 检查（`== null`、`!= null`、Optional）
- 循环边界（零次、一次、多次）
- 提前返回 / guard clause

对比已有测试，列出未覆盖的分支。

**输出：** 覆盖缺口清单 — `[类, 方法, 未覆盖的分支描述]`

---

### 阶段 4：生成测试

根据覆盖缺口生成 TestNG 测试代码。

**参考示例（按需读取）：**
- 基础结构：`${CLAUDE_SKILL_DIR}/examples/testng-basic.java`
- Mockito 模式：`${CLAUDE_SKILL_DIR}/examples/testng-mockito.java`
- 反射注入：`${CLAUDE_SKILL_DIR}/examples/testng-reflection.java`
- 分支覆盖：`${CLAUDE_SKILL_DIR}/examples/testng-branch-coverage.java`

**参考规则：**
- 完整约束清单：`${CLAUDE_SKILL_DIR}/references/testing-rules.md`

**生成规则：**

1. **测试文件位置**：`src/test/java/ut/{被测类包路径}/{被测类名}Test.java`
2. **已有测试文件**：用 Edit 工具追加新测试方法，不破坏已有测试
3. **新建测试文件**：用 Write 工具创建，包含完整的 import、类声明、@BeforeMethod/@AfterMethod
   - **类注释规范**：新增测试类只写测试类描述，不写其他信息。示例：`// FooServiceTest 单元测试`
   - **方法注释规范**：中英文混写时无空格，示例：`// 测试calculateDiscount方法当数量超过100时` 而非 `// 测试 calculateDiscount 方法当数量超过 100 时`
4. **Mock 初始化**：
   - 有 setter 或构造器注入的依赖：用 `@Mock` + `@InjectMocks`
   - 私有字段无 setter：用反射 `injectField()` 注入
   - 静态字段：用 `injectStaticField()` 注入
5. **Mock 行为复用** — 将重复的 `when(...).thenReturn(...)` 抽成 private 方法：
   ```java
   private void mockPaymentSuccess() {
       when(paymentGateway.charge(anyDouble())).thenReturn(true);
   }
   ```
   同理，verify 逻辑重复时也抽成方法：
   ```java
   private void verifyOnlyEmailCalled(String address, String message) {
       verify(emailClient).send(address, message);
       verify(smsClient, never()).send(anyString(), anyString());
   }
   ```
6. **参数化测试优先** — 同一方法的多种输入/分支场景，优先用 `@DataProvider` + `@Test(dataProvider = "xxx")` 参数化覆盖，避免写多个重复的测试方法：
   ```java
   @DataProvider(name = "discountByAmount")
   public Object[][] discountByAmount() {
       return new Object[][]{
           {1500.0, "NORMAL", 1350.0, "大额折扣"},
           {500.0,  "NORMAL", 475.0,  "普通折扣"},
           {1000.0, "NORMAL", 950.0,  "边界值"},
       };
   }

   @Test(dataProvider = "discountByAmount")
   public void testCalculateDiscount(double amount, String type, double expected, String desc) {
       assertEquals(service.calculateDiscount(amount, type), expected, 0.01, desc);
   }
   ```
7. **充分使用 TestNG 注解和特性**：
   - `@Test(description = "...")` — 每个测试方法必须写 description 说明意图
   - `@Test(dataProvider = "...")` — 参数化测试
   - `@Test(expectedExceptions = XxxException.class)` — 异常验证，不要用 try/catch + fail
   - `@Test(expectedExceptionsMessageRegExp = "...")` — 异常消息正则匹配
   - `@Test(groups = "...")` — 按分支类型分组（如 "if-else"、"switch-case"、"exception"）
   - `@Test(timeOut = 3000)` — 性能敏感方法加超时保护
   - `@Test(dependsOnMethods = "...")` — 有前置依赖的测试声明依赖关系
   - `SoftAssert` — 需要验证多个属性时用 SoftAssert 批量断言，一次收集所有失败
   - `ArgumentCaptor` — 需要验证传参细节时用 Mockito 的 ArgumentCaptor 捕获参数
   - `InOrder` — 需要验证调用顺序时用 `inOrder(mock1, mock2)`
8. **断言**：使用 TestNG 的 `assertEquals`、`assertTrue`、`assertNull`、`assertNotNull`、`assertFalse`、`assertThrows` 等，带上 message 参数说明断言意图

**反射注入工具方法模板（写入测试类）：**
```java
private void injectField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
}
```

如果被测类有父类字段需要注入，使用向上查找的版本（参考 `testng-reflection.java` 中的 `findField` 方法）。

---

### 阶段 5：编译运行测试

**检测构建工具：**
```bash
ls pom.xml build.gradle build.gradle.kts 2>/dev/null | head -1
```

**运行测试：**
```bash
# Maven
mvn test -pl <module> -Dtest=<TestClassName> -Dsurefire.useFile=false 2>&1 | tail -100

# Gradle
./gradlew test --tests "<full.qualified.TestClassName>" 2>&1 | tail -100
```

**处理失败：**
- 编译错误：修复 import、类型不匹配等问题，重新运行（最多重试 3 次）
- 断言失败：进入阶段 6 分析是测试问题还是业务 bug
- 如果是测试代码本身的问题（mock 设置不对、断言值写错）：修复测试，重新运行
- 如果测试逻辑正确但结果不符合预期：标记为疑似业务 bug

---

### 阶段 6：Bug 检测与报告

当测试失败且测试逻辑本身正确时（断言符合方法文档/注释/命名所暗示的预期行为，但实际输出不同）：

**分析步骤：**
1. 读取失败测试的断言和实际值
2. 追踪业务代码的执行路径
3. 定位行为偏差的具体代码行
4. 判断是否为业务逻辑错误

**报告格式（参考 `${CLAUDE_SKILL_DIR}/references/bug-report-template.md`）：**

```
## 疑似 Bug #{序号}

**文件**: `<文件路径>`
**方法**: `<方法签名>`
**行号**: <行号>

**期望行为**: <根据方法语义/文档/命名推断的预期行为>
**实际行为**: <代码实际产生的行为>

**测试证据**: `<测试方法名>` 断言失败
**严重程度**: 高/中/低

**分析**: <具体哪行代码导致了偏差，为什么认为是 bug>

> 注意：业务代码未被修改。请开发者人工确认并修复。
```

**重要：** 不要因为测试失败就一定认为是 bug。仔细区分：
- 测试预期写错了（修复测试）
- 业务逻辑确实有问题（报告 bug）
- 需求理解有歧义（在报告中标注不确定，建议人工确认）

---

### 阶段 7：输出报告

所有阶段完成后，输出简洁的汇总报告：

```
## Test Weaver 报告

**分析范围**: <N> 个变更文件，<M> 个变更方法
**对比分支**: master

### 测试生成
- 新建测试文件: <N> 个
- 新增测试方法: <N> 个
- 更新已有测试: <N> 个
- 跳过（不可测）: <N> 个

### 测试结果
- 通过: <N>
- 失败: <N>

### 疑似 Bug
<列出每个疑似 bug 的简要描述，或 "无">

### 跳过项
<列出跳过的方法及原因，或 "无">
```

---

### 阶段 8：恢复模型（如果已切换）

如果在阶段 0 切换过模型，现在恢复原模型：

```bash
if [ "$SWITCHED_MODEL" = true ]; then
    echo "恢复原模型: $ORIGINAL_MODEL"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/\"model\"[[:space:]]*:[[:space:]]*\"[^\"]*\"/\"model\": \"$ORIGINAL_MODEL\"/" "$SETTINGS_FILE"
    else
        sed -i "s/\"model\"[[:space:]]*:[[:space:]]*\"[^\"]*\"/\"model\": \"$ORIGINAL_MODEL\"/" "$SETTINGS_FILE"
    fi
fi
```

## 注意事项

- 如果项目没有 TestNG 依赖，在报告开头提醒用户添加依赖，但仍然生成测试代码
- 如果项目结构不标准（没有 `src/main/java`），尝试自动识别源码目录
- 对于 Lombok 注解的类（`@Data`、`@Builder` 等），按生成后的方法来测试
- 对于 Spring 注解（`@Autowired`、`@Value` 等），用 Mockito mock 或反射注入替代容器注入
