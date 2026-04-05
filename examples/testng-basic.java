package ut.com.example.service;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * 基础 TestNG 测试骨架示例
 * - @BeforeMethod / @AfterMethod 做初始化和清理
 * - 驼峰命名：testMethod{测试内容}
 * - 每个测试方法聚焦一个场景
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

    @Test
    public void testDoSomethingValidInput() {
        String result = fooService.doSomething("hello");
        assertEquals(result, "HELLO");
    }

    @Test
    public void testDoSomethingNullInput() {
        String result = fooService.doSomething(null);
        assertNull(result);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDoSomethingEmptyInputThrowsException() {
        fooService.doSomething("");
    }
}
