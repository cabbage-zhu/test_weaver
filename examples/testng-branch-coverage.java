package ut.com.example.service;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * 分支覆盖示例
 * - @DataProvider 参数化测试覆盖多种输入组合
 * - if/else、switch/case、try/catch、null 检查 各分支都要覆盖
 * - 每个测试方法注释说明覆盖的分支路径
 */
public class DiscountServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private DiscountService discountService;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    // ========== if/else 分支覆盖 ==========

    /** 覆盖分支：amount > 1000 为 true */
    @Test
    public void testCalculateDiscountLargeAmount() {
        double result = discountService.calculateDiscount(1500.0, "NORMAL");
        assertEquals(result, 1500.0 * 0.9, 0.01);
    }

    /** 覆盖分支：amount > 1000 为 false */
    @Test
    public void testCalculateDiscountSmallAmount() {
        double result = discountService.calculateDiscount(500.0, "NORMAL");
        assertEquals(result, 500.0 * 0.95, 0.01);
    }

    // ========== switch/case 分支覆盖 ==========

    @DataProvider(name = "memberTypes")
    public Object[][] memberTypes() {
        return new Object[][]{
                {"VIP", 0.8},       // case "VIP"
                {"GOLD", 0.85},     // case "GOLD"
                {"NORMAL", 0.95},   // case "NORMAL"
                {"UNKNOWN", 1.0},   // default
        };
    }

    /** 覆盖 switch 所有 case + default */
    @Test(dataProvider = "memberTypes")
    public void testGetDiscountRateByMemberType(String memberType, double expectedRate) {
        double rate = discountService.getDiscountRate(memberType);
        assertEquals(rate, expectedRate, 0.01);
    }

    // ========== null 检查分支覆盖 ==========

    /** 覆盖分支：memberType == null */
    @Test
    public void testCalculateDiscountNullMemberType() {
        double result = discountService.calculateDiscount(1000.0, null);
        // null 应走默认折扣逻辑
        assertEquals(result, 1000.0, 0.01);
    }

    // ========== try/catch 分支覆盖 ==========

    /** 覆盖分支：try 正常路径 */
    @Test
    public void testApplyDiscountWithValidMember() {
        when(memberRepository.findById("m-1")).thenReturn(new Member("m-1", "VIP"));

        double result = discountService.applyDiscount("m-1", 1000.0);

        assertEquals(result, 800.0, 0.01);
        verify(memberRepository).findById("m-1");
    }

    /** 覆盖分支：catch 异常路径 — 仓储层抛异常时走降级逻辑 */
    @Test
    public void testApplyDiscountWhenRepositoryFails() {
        when(memberRepository.findById("m-1")).thenThrow(new RuntimeException("DB timeout"));

        double result = discountService.applyDiscount("m-1", 1000.0);

        // 降级：不打折，返回原价
        assertEquals(result, 1000.0, 0.01);
    }

    // ========== 边界值覆盖 ==========

    /** 覆盖边界：amount 恰好等于 1000 */
    @Test
    public void testCalculateDiscountBoundaryAmount() {
        double result = discountService.calculateDiscount(1000.0, "NORMAL");
        // 验证 > 1000 的边界行为（1000 不满足 > 1000）
        assertEquals(result, 1000.0 * 0.95, 0.01);
    }

    /** 覆盖边界：amount 为 0 */
    @Test
    public void testCalculateDiscountZeroAmount() {
        double result = discountService.calculateDiscount(0.0, "VIP");
        assertEquals(result, 0.0, 0.01);
    }

    /** 覆盖边界：amount 为负数 */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCalculateDiscountNegativeAmount() {
        discountService.calculateDiscount(-100.0, "NORMAL");
    }
}
