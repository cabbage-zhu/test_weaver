package ut.com.example.service;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * 反射注入示例 — 替代 PowerMock
 *
 * 要点：
 * - injectField / injectStaticField 抽成可复用工具方法
 * - @DataProvider 参数化覆盖不同渠道的发送逻辑
 * - mock 行为抽成方法，减少重复 when/verify
 * - 适用于私有字段无 setter、静态字段、@InjectMocks 无法覆盖的场景
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
        injectField(notificationService, "emailClient", emailClient);
        injectField(notificationService, "smsClient", smsClient);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    // ========== 反射注入工具方法（可复用） ==========

    /**
     * 通用反射注入 — 支持私有字段，向上查找父类
     */
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 静态字段注入 — 替代 PowerMock.mockStatic()
     */
    private void injectStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

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

    // ========== 可复用的 mock 行为 ==========

    private void mockEmailSend(boolean result) {
        when(emailClient.send(anyString(), anyString())).thenReturn(result);
    }

    private void mockSmsSend(boolean result) {
        when(smsClient.send(anyString(), anyString())).thenReturn(result);
    }

    private void verifyOnlyEmailCalled(String address, String message) {
        verify(emailClient).send(address, message);
        verify(smsClient, never()).send(anyString(), anyString());
    }

    private void verifyOnlySmsCalled(String phone, String message) {
        verify(smsClient).send(phone, message);
        verify(emailClient, never()).send(anyString(), anyString());
    }

    private void verifyNeitherCalled() {
        verify(emailClient, never()).send(anyString(), anyString());
        verify(smsClient, never()).send(anyString(), anyString());
    }

    // ========== DataProvider 参数化覆盖渠道分支 ==========

    @DataProvider(name = "sendSuccessScenarios")
    public Object[][] sendSuccessScenarios() {
        return new Object[][]{
                // address,             message,  channel, description
                {"user@example.com",    "Hello",  "EMAIL", "邮件渠道发送成功"},
                {"13800138000",         "Hello",  "SMS",   "短信渠道发送成功"},
        };
    }

    @Test(dataProvider = "sendSuccessScenarios",
          description = "参数化验证不同渠道的发送成功场景")
    public void testSendNotificationSuccess(String address, String message,
                                            String channel, String desc) {
        if ("EMAIL".equals(channel)) {
            mockEmailSend(true);
        } else {
            mockSmsSend(true);
        }

        boolean result = notificationService.sendNotification(address, message, channel);

        assertTrue(result, desc);
        if ("EMAIL".equals(channel)) {
            verifyOnlyEmailCalled(address, message);
        } else {
            verifyOnlySmsCalled(address, message);
        }
    }

    // ========== 未知渠道 ==========

    @DataProvider(name = "unknownChannels")
    public Object[][] unknownChannels() {
        return new Object[][]{
                {"FAX"}, {"PUSH"}, {""}, {null},
        };
    }

    @Test(dataProvider = "unknownChannels",
          description = "未知或空渠道应返回 false 且不调用任何客户端")
    public void testSendNotificationUnknownChannel(String channel) {
        boolean result = notificationService.sendNotification("addr", "Hello", channel);

        assertFalse(result);
        verifyNeitherCalled();
    }

    // ========== 发送失败场景 ==========

    @Test(description = "邮件发送失败应返回 false")
    public void testSendNotificationEmailFailed() {
        mockEmailSend(false);

        boolean result = notificationService.sendNotification("user@example.com", "Hello", "EMAIL");

        assertFalse(result);
    }
}
