package ut.com.example.service;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.*;

/**
 * 基础 TestNG 测试骨架示例
 *
 * 要点：
 * - @DataProvider 参数化覆盖多场景，避免重复测试方法
 * - SoftAssert 批量断言，一次收集所有失败
 * - @Test 属性：description 说明意图，expectedExceptions 验证异常，
 *   groups 分组，timeOut 超时保护
 * - 驼峰命名：testMethod{测试内容}
 */
public class FooServiceTest {

    private FooService fooService;

    @BeforeMethod
    public void setUp() {
        fooService = new FooService();
    }

    @AfterMethod
    public void tearDown() {
        fooService = null;
    }

    // ========== DataProvider 参数化测试 ==========

    @DataProvider(name = "validInputs")
    public Object[][] validInputs() {
        return new Object[][]{
                {"hello", "HELLO"},
                {"world", "WORLD"},
                {"Java", "JAVA"},
                {"a", "A"},
        };
    }

    @Test(dataProvider = "validInputs", description = "验证合法输入的大写转换")
    public void testDoSomethingValidInput(String input, String expected) {
        assertEquals(fooService.doSomething(input), expected);
    }

    // ========== 异常场景 ==========

    @DataProvider(name = "invalidInputs")
    public Object[][] invalidInputs() {
        return new Object[][]{
                {""},
                {"   "},
        };
    }

    @Test(dataProvider = "invalidInputs",
          expectedExceptions = IllegalArgumentException.class,
          description = "空白输入应抛出 IllegalArgumentException")
    public void testDoSomethingInvalidInput(String input) {
        fooService.doSomething(input);
    }

    // ========== null 检查 ==========

    @Test(description = "null 输入应返回 null")
    public void testDoSomethingNullInput() {
        assertNull(fooService.doSomething(null));
    }

    // ========== SoftAssert 批量断言示例 ==========

    @Test(description = "批量验证多个属性，收集所有失败后统一报告")
    public void testProcessResultFields() {
        Result result = fooService.process("input");

        SoftAssert sa = new SoftAssert();
        sa.assertNotNull(result, "result should not be null");
        sa.assertEquals(result.getCode(), 200, "code should be 200");
        sa.assertEquals(result.getMessage(), "OK", "message should be OK");
        sa.assertTrue(result.isSuccess(), "success should be true");
        sa.assertAll();
    }

    // ========== 超时保护 ==========

    @Test(timeOut = 3000, description = "验证方法在 3 秒内完成")
    public void testDoSomethingPerformance() {
        String result = fooService.doSomething("performance");
        assertNotNull(result);
    }
}
