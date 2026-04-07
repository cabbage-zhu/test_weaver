package ut.com.example.service;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Mockito + TestNG 示例
 *
 * 要点：
 * - 可复用的 mock 行为抽成 private 方法，减少重复 when/thenReturn
 * - @DataProvider 参数化覆盖多种 mock 场景
 * - ArgumentCaptor 捕获并验证传参细节
 * - 绝不使用 PowerMock
 */
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderService orderService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterMethod
    public void tearDown() {
    }

    // ========== 可复用的 mock 行为 ==========

    private void mockPaymentSuccess() {
        when(paymentGateway.charge(anyDouble())).thenReturn(true);
    }

    private void mockPaymentFailed() {
        when(paymentGateway.charge(anyDouble())).thenReturn(false);
    }

    private void mockSaveOrder(Order order) {
        when(orderRepository.save(any(Order.class))).thenReturn(order);
    }

    private void mockSaveOrderThrows(RuntimeException ex) {
        when(orderRepository.save(any(Order.class))).thenThrow(ex);
    }

    private Order createOrder(String itemId, double amount) {
        return new Order(itemId, amount);
    }

    // ========== DataProvider 参数化测试 ==========

    @DataProvider(name = "placeOrderScenarios")
    public Object[][] placeOrderScenarios() {
        return new Object[][]{
                // itemId, amount, paymentResult, expectSaved
                {"item-1", 100.0, true, true},
                {"item-2", 200.0, true, true},
                {"item-3", 50.0, false, false},
        };
    }

    @Test(dataProvider = "placeOrderScenarios", description = "参数化验证下单流程：支付成功/失败")
    public void testPlaceOrder(String itemId, double amount, boolean paymentOk, boolean expectSaved) {
        Order order = createOrder(itemId, amount);
        when(paymentGateway.charge(amount)).thenReturn(paymentOk);
        if (paymentOk) {
            mockSaveOrder(order);
        }

        Order result = orderService.placeOrder(order);

        if (expectSaved) {
            assertNotNull(result);
            verify(orderRepository).save(order);
        } else {
            assertNull(result);
            verify(orderRepository, never()).save(any());
        }
        verify(paymentGateway).charge(amount);
    }

    // ========== ArgumentCaptor 验证传参 ==========

    @Test(description = "验证 save 时传入的 Order 状态已更新为 PAID")
    public void testPlaceOrderSetsStatusToPaid() {
        Order order = createOrder("item-1", 100.0);
        mockPaymentSuccess();
        mockSaveOrder(order);

        orderService.placeOrder(order);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertEquals(captor.getValue().getStatus(), "PAID");
    }

    // ========== 异常路径 ==========

    @Test(expectedExceptions = RuntimeException.class,
          expectedExceptionsMessageRegExp = "DB error",
          description = "仓储层异常应向上抛出")
    public void testPlaceOrderRepositoryThrowsException() {
        Order order = createOrder("item-1", 100.0);
        mockPaymentSuccess();
        mockSaveOrderThrows(new RuntimeException("DB error"));

        orderService.placeOrder(order);
    }

    // ========== verify 调用顺序 ==========

    @Test(description = "验证先扣款再保存的调用顺序")
    public void testPlaceOrderCallSequence() {
        Order order = createOrder("item-1", 100.0);
        mockPaymentSuccess();
        mockSaveOrder(order);

        orderService.placeOrder(order);

        var inOrder = inOrder(paymentGateway, orderRepository);
        inOrder.verify(paymentGateway).charge(100.0);
        inOrder.verify(orderRepository).save(order);
    }
}
