package ut.com.example.service;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Mockito + TestNG 示例
 * - 使用 @Mock 创建 mock 对象
 * - 使用 @InjectMocks 自动注入依赖
 * - MockitoAnnotations.openMocks() 初始化
 * - 绝不使用 PowerMock
 */
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderService orderService;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void testPlaceOrderPaymentSuccess() {
        Order order = new Order("item-1", 100);
        when(paymentGateway.charge(anyDouble())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.placeOrder(order);

        assertNotNull(result);
        verify(paymentGateway).charge(100.0);
        verify(orderRepository).save(order);
    }

    @Test
    public void testPlaceOrderPaymentFailed() {
        Order order = new Order("item-1", 100);
        when(paymentGateway.charge(anyDouble())).thenReturn(false);

        Order result = orderService.placeOrder(order);

        assertNull(result);
        verify(paymentGateway).charge(100.0);
        verify(orderRepository, never()).save(any());
    }

    @Test
    public void testPlaceOrderRepositoryThrowsException() {
        Order order = new Order("item-1", 100);
        when(paymentGateway.charge(anyDouble())).thenReturn(true);
        when(orderRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        try {
            orderService.placeOrder(order);
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "DB error");
        }

        verify(paymentGateway).charge(100.0);
    }
}
