package ut.com.example.service;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * 反射注入示例 — 替代 PowerMock
 *
 * 场景：被测类持有一个私有字段（可能是 static 或通过构造器/setter 无法注入的依赖），
 * 通过 java.lang.reflect.Field 直接设置 mock 对象。
 *
 * 适用于：
 * 1. 私有字段没有 setter 且不通过构造器注入
 * 2. 静态字段持有的工具类/客户端实例
 * 3. @InjectMocks 无法覆盖的场景
 */
public class NotificationServiceTest {

    @Mock
    private EmailClient emailClient;

    @Mock
    private SmsClient smsClient;

    private NotificationService notificationService;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService();

        // 通过反射注入私有字段 emailClient
        injectField(notificationService, "emailClient", emailClient);

        // 通过反射注入私有字段 smsClient
        injectField(notificationService, "smsClient", smsClient);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    /**
     * 通用反射注入工具方法
     * 支持注入私有字段、静态字段
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 向上查找字段（支持父类中的字段）
     */
    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in " + clazz.getName() + " or its parents");
    }

    /**
     * 注入静态字段的变体
     * 用于替代 PowerMock.mockStatic() 的场景
     */
    private void injectStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value); // null target = static field
    }

    @Test
    public void testSendNotificationViaEmail() {
        when(emailClient.send(anyString(), anyString())).thenReturn(true);

        boolean result = notificationService.sendNotification("user@example.com", "Hello", "EMAIL");

        assertTrue(result);
        verify(emailClient).send("user@example.com", "Hello");
        verify(smsClient, never()).send(anyString(), anyString());
    }

    @Test
    public void testSendNotificationViaSms() {
        when(smsClient.send(anyString(), anyString())).thenReturn(true);

        boolean result = notificationService.sendNotification("13800138000", "Hello", "SMS");

        assertTrue(result);
        verify(smsClient).send("13800138000", "Hello");
        verify(emailClient, never()).send(anyString(), anyString());
    }

    @Test
    public void testSendNotificationUnknownChannel() {
        boolean result = notificationService.sendNotification("addr", "Hello", "FAX");

        assertFalse(result);
        verify(emailClient, never()).send(anyString(), anyString());
        verify(smsClient, never()).send(anyString(), anyString());
    }
}
