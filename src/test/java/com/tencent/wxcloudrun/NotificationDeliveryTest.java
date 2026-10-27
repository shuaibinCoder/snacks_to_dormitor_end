package com.tencent.wxcloudrun;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import com.tencent.wxcloudrun.service.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.TEMPLATE_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationDeliveryTest {
  private NotificationMapper notifications;
  private NotificationDeliveryMapper deliveries;
  private NotificationTemplateMapper templates;
  private NotificationGrantMapper grants;
  private NotificationPreferenceMapper preferences;
  private WechatMessageClient client;
  private NotificationDeliveryService service;
  private Notification notification;
  private NotificationDelivery delivery;
  private NotificationGrant grant;
  private NotificationTemplate config;

  @BeforeEach
  void setup() {
    notifications = mock(NotificationMapper.class);
    deliveries = mock(NotificationDeliveryMapper.class);
    templates = mock(NotificationTemplateMapper.class);
    grants = mock(NotificationGrantMapper.class);
    preferences = mock(NotificationPreferenceMapper.class);
    client = mock(WechatMessageClient.class);
    service = new NotificationDeliveryService(notifications, deliveries, templates, grants, preferences, client,
        new TestTransactions());
    notification = new Notification();
    notification.setId("notice");
    notification.setRecipientOpenid("buyer");
    notification.setSubscriptionRole("BUYER");
    notification.setSubscriptionGrantId("grant");
    notification.setRequireSpecificGrant(true);
    notification.setBizId("order");
    notification.setStatusText("已送达");
    notification.setSummary("测试");
    notification.setCreatedAt(1);
    delivery = new NotificationDelivery();
    delivery.setId("delivery");
    delivery.setDeliveryStatus("PENDING");
    grant = new NotificationGrant();
    grant.setId("grant");
    grant.setOpenid("buyer");
    grant.setSubscriptionRole("BUYER");
    grant.setTemplateKey(TEMPLATE_KEY);
    grant.setScene("ORDER_SUBMIT");
    grant.setBoundBizType("ORDER");
    grant.setBoundBizId("order");
    grant.setStatus("AVAILABLE");
    config = new NotificationTemplate();
    config.setEnabled(true);
    config.setTemplateId("template");
    config.setFields(of("titleKey", "phrase8", "statusKey", "phrase23", "timeKey", "time3", "remarkKey", "thing2"));
    NotificationPreference preference = new NotificationPreference();
    preference.setId("pref");
    preference.setGrantedCount(1);
    when(notifications.selectById("notice", true)).thenReturn(notification);
    when(deliveries.selectByNotification("notice", "WECHAT_SUBSCRIBE", true)).thenReturn(delivery);
    when(templates.selectByKey(TEMPLATE_KEY)).thenReturn(config);
    when(grants.selectById("grant", true)).thenReturn(grant);
    when(preferences.select(eq("buyer"), eq(TEMPLATE_KEY), eq("BUYER"), eq(true))).thenReturn(preference);
    when(grants.claim(any(NotificationGrant.class), eq(true), eq("order"))).thenAnswer(i -> {
      NotificationGrant expected = i.getArgument(0);
      if (!"AVAILABLE".equals(grant.getStatus()) || !expected.getOpenid().equals(grant.getOpenid())
          || !"order".equals(grant.getBoundBizId())) {
        return 0;
      }
      grant.setStatus("SENDING");
      return 1;
    });
    when(deliveries.claimPending(any())).thenAnswer(i -> {
      // 服务层会先把参数对象标为 SENDING；Mapper 的 SQL 仍在数据库条件中校验 PENDING。
      if (!"SENDING".equals(delivery.getDeliveryStatus())) {
        return 0;
      }
      return 1;
    });
    when(grants.finish(eq("grant"), anyString(), anyLong(), anyLong())).thenAnswer(i -> {
      if (!"SENDING".equals(grant.getStatus())) {
        return 0;
      }
      grant.setStatus(i.getArgument(1));
      return 1;
    });
    when(deliveries.finish(any())).thenAnswer(i -> {
      if (!"SENDING".equals(delivery.getDeliveryStatus())) {
        return 0;
      }
      NotificationDelivery result = i.getArgument(0);
      delivery.setDeliveryStatus(result.getDeliveryStatus());
      return 1;
    });
    when(deliveries.skip(eq("delivery"), anyString(), anyLong())).thenAnswer(i -> {
      delivery.setDeliveryStatus("SKIPPED");
      return 1;
    });
    when(client.send(any(Data.class))).thenReturn(of("errcode", 0));
  }

  @Test
  void claimBeforeSendAndNoRepeatAfterSuccess() {
    when(client.send(any(Data.class))).thenAnswer(i -> {
      assertEquals("SENDING", grant.getStatus());
      assertEquals("SENDING", delivery.getDeliveryStatus());
      return of("errcode", 0);
    });
    assertTrue(service.send("notice"));
    assertFalse(service.send("notice"));
    assertEquals("CONSUMED", grant.getStatus());
    assertEquals("SUCCESS", delivery.getDeliveryStatus());
    verify(client, times(1)).send(any(Data.class));
  }

  @Test
  void refusedOrUncertainResultIsRecordedWithoutRetry() {
    when(client.send(any(Data.class))).thenReturn(of("errcode", 43101, "errmsg", "refused"));
    assertFalse(service.send("notice"));
    assertEquals("FAILED_FINAL", delivery.getDeliveryStatus());
    assertEquals("FAILED", grant.getStatus());
    assertFalse(service.send("notice"));
    verify(client, times(1)).send(any(Data.class));
  }

  @Test
  void timeoutLeavesUnknownAndNeverPretendsSuccess() {
    when(client.send(any(Data.class))).thenThrow(new IllegalStateException("timeout"));
    assertFalse(service.send("notice"));
    assertEquals("UNKNOWN", delivery.getDeliveryStatus());
    assertEquals("UNKNOWN", grant.getStatus());
  }

  @Test
  void disabledTemplateSkipsExternalCalls() {
    config.setEnabled(false);
    assertFalse(service.send("notice"));
    assertEquals("SKIPPED", delivery.getDeliveryStatus());
    verifyNoInteractions(client);
    assertEquals("AVAILABLE", grant.getStatus());
  }

  @Test
  void missingOrOtherOrderGrantCannotUseHistoricalPermission() {
    notification.setSubscriptionGrantId("");
    assertFalse(service.send("notice"));
    verifyNoInteractions(client);
    delivery.setDeliveryStatus("PENDING");
    notification.setSubscriptionGrantId("grant");
    grant.setBoundBizId("different-order");
    assertFalse(service.send("notice"));
    verifyNoInteractions(client);
  }

  /** 单元测试事务替身只验证调用顺序，真实行锁由 SnacksMysqlIT 验证。 */
  static class TestTransactions extends AbstractPlatformTransactionManager {
    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
