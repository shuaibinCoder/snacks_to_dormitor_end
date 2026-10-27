package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.time.format.DateTimeFormatter;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 微信订阅消息的独立事务领取、外部发送和最终诊断记录。 */
@Service
public class NotificationDeliveryService {
  private final NotificationMapper notifications;
  private final NotificationDeliveryMapper deliveries;
  private final NotificationTemplateMapper templates;
  private final NotificationGrantMapper grants;
  private final NotificationPreferenceMapper preferences;
  private final WechatMessageClient client;
  private final TransactionTemplate tx;

  public NotificationDeliveryService(NotificationMapper notifications, NotificationDeliveryMapper deliveries,
      NotificationTemplateMapper templates, NotificationGrantMapper grants, NotificationPreferenceMapper preferences,
      WechatMessageClient client, PlatformTransactionManager manager) {
    this.notifications = notifications;
    this.deliveries = deliveries;
    this.templates = templates;
    this.grants = grants;
    this.preferences = preferences;
    this.client = client;
    tx = new TransactionTemplate(manager);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** 先提交 SENDING 再调用微信；异常结果记为 UNKNOWN，绝不伪造成功或自动重发。 */
  public boolean send(String notificationId) {
    Data claim = tx.execute(status -> claim(notificationId));
    if (claim == null) {
      return false;
    }
    Data response;
    try {
      response = client.send(payload(claim.object("notification"), claim.object("config")));
    } catch (RuntimeException error) {
      response = of("errcode", WechatResultCode.UNKNOWN.code(), "errmsg", "微信发送结果未知，不自动重发");
    }
    final Data result = response;
    tx.execute(status -> {
      finish(claim, result);
      return null;
    });
    return WechatResultCode.SUCCESS.matches(result.text("errcode"));
  }

  /** 授权与投递记录都在同一短事务原子领取，避免两个发送者同时使用凭证。 */
  private Data claim(String id) {
    Notification notification = notifications.selectById(id, true);
    require(notification != null, "记录不存在");
    NotificationDelivery delivery = deliveries.selectByNotification(id, NotificationChannel.WECHAT_SUBSCRIBE.code(), true);
    if (delivery == null || !DeliveryStatus.PENDING.matches(delivery.getDeliveryStatus())) {
      return null;
    }
    NotificationTemplate config = templates.selectByKey(TEMPLATE_KEY);
    require(config != null, "通知模板尚未初始化");
    NotificationGrant grant = notification.getSubscriptionGrantId().isEmpty()
        ? notification.isRequireSpecificGrant()
            ? null
            : grants.selectAvailableForTest(notification.getRecipientOpenid(), notification.getSubscriptionRole(), TEMPLATE_KEY)
        : grants.selectById(notification.getSubscriptionGrantId(), true);
    if (!config.isEnabled() || config.getTemplateId().isEmpty() || grant == null) {
      return skip(delivery);
    }

    NotificationGrant expected = new NotificationGrant();
    expected.setId(grant.getId());
    expected.setOpenid(notification.getRecipientOpenid());
    expected.setTemplateKey(TEMPLATE_KEY);
    expected.setSubscriptionRole(notification.getSubscriptionRole());
    expected.setUpdatedAt(now());
    int claimed = grants.claim(expected, notification.isRequireSpecificGrant(), notification.getBizId());
    if (claimed != 1) {
      return skip(delivery);
    }
    NotificationPreference preference = preferences.select(notification.getRecipientOpenid(), TEMPLATE_KEY,
        notification.getSubscriptionRole(), true);
    if (preference != null) {
      preferences.consumeGrant(preference.getId(), now());
    }
    delivery.setTemplateId(config.getTemplateId());
    delivery.setSubscriptionGrantId(grant.getId());
    delivery.setDeliveryStatus(DeliveryStatus.SENDING.code());
    require(deliveries.claimPending(Entities.update(delivery)) == 1, "通知投递已被其他任务领取");
    return of("notification", Entities.snapshot(notification), "config", Entities.snapshot(config),
        "deliveryId", delivery.getId(), "grantId", grant.getId(),
        "preferenceId", preference == null ? "" : preference.getId());
  }

  private Data skip(NotificationDelivery delivery) {
    deliveries.skip(delivery.getId(), "模板未开启或没有匹配本次业务的授权", now());
    return null;
  }

  /** 只有已领取的 SENDING 记录才能进入成功、明确失败或未知终态。 */
  private void finish(Data claim, Data result) {
    boolean success = WechatResultCode.SUCCESS.matches(result.text("errcode"));
    boolean unknown = WechatResultCode.UNKNOWN.matches(result.text("errcode"));
    String grantStatus = success
        ? GrantStatus.CONSUMED.code()
        : unknown ? GrantStatus.UNKNOWN.code() : GrantStatus.FAILED.code();
    require(grants.finish(claim.text("grantId"), grantStatus, success ? now() : 0, now()) == 1, "订阅授权状态已变化");
    NotificationDelivery delivery = new NotificationDelivery();
    delivery.setId(claim.text("deliveryId"));
    delivery.setDeliveryStatus(success
        ? DeliveryStatus.SUCCESS.code()
        : unknown ? DeliveryStatus.UNKNOWN.code() : DeliveryStatus.FAILED_FINAL.code());
    delivery.setLastErrorCode(success ? "" : result.text("errcode"));
    delivery.setLastErrorMessage(success ? "" : truncate(result.text("errmsg"), ERROR_DETAIL_LENGTH));
    delivery.setSentAt(success ? now() : 0);
    require(deliveries.finish(Entities.update(delivery)) == 1, "通知投递状态已变化");
    if (WechatResultCode.REFUSED.matches(result.text("errcode")) && !claim.text("preferenceId").isEmpty()) {
      preferences.clearGrants(claim.text("preferenceId"), SubscribePermission.REJECT.code(), now());
    }
  }

  /** 按 Unicode 码点截断微信长度受限字段，避免截断 Emoji  surrogate 对。 */
  static String truncate(String value, int limit) {
    int count = value.codePointCount(0, value.length());
    return count <= limit ? value : value.substring(0, value.offsetByCodePoints(0, limit));
  }

  /** 将内部通知映射成微信 subscribe/send 所需字段，不直接透传数据库实体。 */
  public static Data payload(Data notification, Data config) {
    Data fields = config.object("fields");
    Data data = new Data();
    String title = NotificationType.ORDER_ACCEPTED.matches(notification.text("type"))
        ? "接单提醒"
        : NotificationType.ORDER_DELIVERED.matches(notification.text("type")) ? "送达提醒" : "测试通知";
    data.put(fields.text("titleKey"), of("value", truncate(title, WECHAT_SHORT_FIELD_LENGTH)));
    data.put(fields.text("statusKey"),
        of("value", truncate(notification.text("statusText").replaceAll("[\\r\\n]", ""), WECHAT_SHORT_FIELD_LENGTH)));
    data.put(fields.text("timeKey"), of("value", DateTimeFormatter.ofPattern(WECHAT_TIME_PATTERN)
        .withZone(ZoneId.of(CHINA_TIME_ZONE))
        .format(Instant.ofEpochMilli(notification.number("createdAt")))));
    data.put(fields.text("remarkKey"), of("value", truncate(notification.text("summary"), WECHAT_REMARK_LENGTH)));
    return of("touser", notification.text("recipientOpenid"), "template_id", config.text("templateId"),
        "miniprogram_state", config.text("miniprogramState", MiniProgramState.TRIAL.code()), "lang", WECHAT_LANGUAGE,
        "page", notification.text("pagePath").replaceFirst("^/", ""), "data", data);
  }
}
