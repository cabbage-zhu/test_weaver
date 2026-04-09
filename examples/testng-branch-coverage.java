package ut.com.example.service;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * 分支覆盖示例
 *
 * 要点：
 * - @DataProvider 参数化覆盖 if/else、switch/case、边界值，一个测试方法覆盖多条分支
 * - mock 行为抽成可复用方法
 * - SoftAssert 批量验证多个断言
 * - @Test(groups) 按分支类型分组，方便按组运行
 * - @Test(expectedExceptions) 验证异常分支
 */
public class DiscountServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private DiscountService discountService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterMethod
    public void tearDown() {
    }

    // ========== 可复用的 mock 行为 ==========

    private void mockFindMember(String memberId, String memberType) {
        when(memberRepository.findById(memberId)).thenReturn(new Member(memberId, memberType));
    }

    private void mockFindMemberThrows(String memberId, RuntimeException ex) {
        when(memberRepository.findById(memberId)).thenThrow(ex);
    }

    // ========== if/else + 边界值：用 DataProvider 一次覆盖 ==========

    @DataProvider(name = "discountByAmount")
    public Object[][] discountByAmount() {
        return new Object[][]{
                // amount,    memberType, expected,           description
                {1500.0,      "NORMAL",   1500.0 * 0.9,      "amount > 1000 走大额折扣"},
                {500.0,       "NORMAL",   500.0 * 0.95,       "amount <= 1000 走普通折扣"},
                {1000.0,      "NORMAL",   1000.0 * 0.95,      "边界值：恰好 1000 不满足 > 1000"},
                {0.0,         "VIP",      0.0,                 "边界值：amount 为 0"},
                {1000.01,     "NORMAL",   1000.01 * 0.9,       "边界值：刚超过 1000"},
        };
    }

    @Test(dataProvider = "discountByAmount", groups = "if-else",
          description = "参数化验证金额阈值的 if/else 分支 + 边界值")
    public void testCalculateDiscountByAmount(double amount, String memberType,
                                              double expected, String desc) {
        assertEquals(discountService.calculateDiscount(amount, memberType), expected, 0.01, desc);
    }

    // ========== switch/case：DataProvider 覆盖所有 case + default ==========

    @DataProvider(name = "memberTypes")
    public Object[][] memberTypes() {
        return new Object[][]{
                {"VIP",     0.8},       // case "VIP"
                {"GOLD",    0.85},      // case "GOLD"
                {"NORMAL",  0.95},      // case "NORMAL"
                {"UNKNOWN", 1.0},       // default
        };
    }

    @Test(dataProvider = "memberTypes", groups = "switch-case",
          description = "参数化验证 switch 所有 case + default")
    public void testGetDiscountRateByMemberType(String memberType, double expectedRate) {
        assertEquals(discountService.getDiscountRate(memberType), expectedRate, 0.01);
    }

    // ========== null 检查 ==========

    @Test(groups = "null-check", description = "memberType 为 null 时走默认折扣")
    public void testCalculateDiscountNullMemberType() {
        assertEquals(discountService.calculateDiscount(1000.0, null), 1000.0, 0.01);
    }

    // ========== try/catch 分支：DataProvider 覆盖正常 + 异常路径 ==========

    @DataProvider(name = "applyDiscountScenarios")
    public Object[][] applyDiscountScenarios() {
        return new Object[][]{
                // memberId, memberType, throwEx, expectedAmount, description
                {"m-1", "VIP",  false, 800.0,  "正常路径：VIP 会员打 8 折"},
                {"m-2", "GOLD", false, 850.0,  "正常路径：GOLD 会员打 85 折"},
        };
    }

    @Test(dataProvider = "applyDiscountScenarios", groups = "try-catch",
          description = "参数化验证 try 正常路径的不同会员类型")
    public void testApplyDiscountNormalPath(String memberId, String memberType,
                                            boolean throwEx, double expected, String desc) {
        mockFindMember(memberId, memberType);

        double result = discountService.applyDiscount(memberId, 1000.0);

        assertEquals(result, expected, 0.01, desc);
        verify(memberRepository).findById(memberId);
    }

    @Test(groups = "try-catch", description = "catch 异常路径：仓储层异常时降级返回原价")
    public void testApplyDiscountWhenRepositoryFails() {
        mockFindMemberThrows("m-1", new RuntimeException("DB timeout"));

        double result = discountService.applyDiscount("m-1", 1000.0);

        assertEquals(result, 1000.0, 0.01, "降级：不打折，返回原价");
    }

    // ========== 异常分支 ==========

    @Test(expectedExceptions = IllegalArgumentException.class,
          groups = "exception",
          description = "负数金额应抛出 IllegalArgumentException")
    public void testCalculateDiscountNegativeAmount() {
        discountService.calculateDiscount(-100.0, "NORMAL");
    }

    // ========== SoftAssert 批量验证 ==========

    @Test(groups = "soft-assert", description = "批量验证折扣计算结果的多个属性")
    public void testApplyDiscountResultFields() {
        mockFindMember("m-1", "VIP");

        DiscountResult result = discountService.applyDiscountDetail("m-1", 1000.0);

        SoftAssert sa = new SoftAssert();
        sa.assertNotNull(result, "result should not be null");
        sa.assertEquals(result.getOriginalAmount(), 1000.0, 0.01);
        sa.assertEquals(result.getDiscountRate(), 0.8, 0.01);
        sa.assertEquals(result.getFinalAmount(), 800.0, 0.01);
        sa.assertEquals(result.getMemberType(), "VIP");
        sa.assertAll();
    }
}
