package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import java.util.*;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 站内通知、订阅授权记录及事务提交后的微信投递调度。 */
@Service
public class NotificationService {
  private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
  private final NotificationTemplateMapper templates;
  private final NotificationPreferenceMapper preferences;
  private final NotificationGrantMapper grants;
  private final NotificationMapper notifications;
  private final NotificationDeliveryMapper deliveries;
  private final PlatformUserMapper platformUsers;
  private final NotificationDeliveryService delivery;
  private final Labels labels;
  private final IdentityService identity;
  private final EntityView view;

  public NotificationService(NotificationTemplateMapper templates, NotificationPreferenceMapper preferences,
      NotificationGrantMapper grants, NotificationMapper notifications, NotificationDeliveryMapper deliveries,
      PlatformUserMapper platformUsers, NotificationDeliveryService delivery, Labels labels,
      IdentityService identity, EntityView view) {
    this.templates = templates;
    this.preferences = preferences;
    this.grants = grants;
    this.notifications = notifications;
    this.deliveries = deliveries;
    this.platformUsers = platformUsers;
    this.delivery = delivery;
    this.labels = labels;
    this.identity = identity;
    this.view = view;
  }

  /** 读取固定统一模板；未初始化时明确失败，不创建客户端指定模板。 */
  public NotificationTemplate config() {
    NotificationTemplate config = templates.selectByKey(TEMPLATE_KEY);
    require(config != null, "通知模板尚未初始化");
    return config;
  }

  /** 管理端返回完整模板及中文节点标签。 */
  public Data adminConfig() {
    return view.data(config());
  }

  /** 用户端只能看到启用状态和必要说明，不暴露内部策略版本。 */
  public Data publicConfig() {
    NotificationTemplate config = config();
    Data data = view.data(config);
    return data.pick("templateId", "enabled", "keyLabel", "description",
        "buyerOrderPushStage", "buyerOrderPushStageLabel")
        .with("templateKey", TEMPLATE_KEY, "enabled", config.isEnabled() && !config.getTemplateId().isEmpty());
  }

  /** 管理员配置模板关键词和买家推送节点，审计与配置共用事务。 */
  public Data saveConfig(Data input, String openid) {
    PlatformUser operator = identity.requirePlatform(openid, codes(PlatformRole.ADMIN, PlatformRole.SUPER));
    NotificationTemplate old = config();
    Data before = Entities.snapshot(old);
    String stage = input.text("buyerOrderPushStage", BuyerOrderPushStage.DELIVERED.code());
    String state = input.text("miniprogramState", MiniProgramState.TRIAL.code());
    require(oneOf(stage, codes(BuyerOrderPushStage.ACCEPTED, BuyerOrderPushStage.DELIVERED))
        && oneOf(state, codes(MiniProgramState.DEVELOPER, MiniProgramState.TRIAL, MiniProgramState.FORMAL)),
        "通知节点或版本不正确");
    Data fields = input.object("fields");
    Set<String> keys = new HashSet<>();
    for (String key : Arrays.asList("titleKey", "statusKey", "timeKey", "remarkKey")) {
      String value = fields.required(key, 50);
      require(value.matches(TEMPLATE_FIELD_PATTERN) && keys.add(value), "模板字段必须有效且不能重复");
    }
    require(!input.flag("enabled") || !input.text("templateId").isEmpty(), "请先填写模板 ID");
    old.setTemplateId(input.text("templateId"));
    old.setEnabled(input.flag("enabled"));
    old.setMiniprogramState(state);
    old.setBuyerOrderPushStage(stage);
    old.setFields(fields.pick("titleKey", "statusKey", "timeKey", "remarkKey"));
    require(templates.updateConfig(Entities.update(old)) == 1, "通知模板保存失败");
    identity.audit(operator, AuditAction.SAVE_NOTIFICATION_TEMPLATE.code(),
        AuditTargetType.NOTIFICATION_TEMPLATE.code(), TEMPLATE_KEY, before,
        of("templateId", old.getTemplateId(), "enabled", old.isEnabled(), "buyerOrderPushStage", stage),
        "配置订阅消息模板");
    return view.data(config());
  }

  /** 每次 ACCEPT 生成独立一次性授权，其他结果只记录最近偏好。 */
  public Data permission(Data e, String openid) {
    String role = e.text("subscriptionRole", SubscriptionRole.GENERAL.code());
    String permission = e.text("permission", SubscribePermission.UNKNOWN.code());
    String scene = e.text("scene");
    require(oneOf(role, codes(SubscriptionRole.BUYER, SubscriptionRole.MERCHANT,
        SubscriptionRole.APPLICANT, SubscriptionRole.PLATFORM, SubscriptionRole.GENERAL)), "订阅身份不正确");
    require(oneOf(permission, codes(SubscribePermission.ACCEPT, SubscribePermission.REJECT,
        SubscribePermission.BAN, SubscribePermission.FILTER, SubscribePermission.UNKNOWN)), "订阅结果不正确");
    NotificationPreference created = new NotificationPreference();
    created.setOpenid(openid);
    created.setTemplateKey(TEMPLATE_KEY);
    created.setSubscriptionRole(role);
    preferences.insertIgnore(Entities.create(created));
    NotificationPreference preference = preferences.select(openid, TEMPLATE_KEY, role, true);
    require(preference != null, "订阅偏好初始化失败");
    String grantId = "";
    if (SubscribePermission.ACCEPT.matches(permission)) {
      NotificationGrant grant = new NotificationGrant();
      grant.setOpenid(openid);
      grant.setTemplateKey(TEMPLATE_KEY);
      grant.setSubscriptionRole(role);
      grant.setScene(scene);
      grant.setStatus(GrantStatus.AVAILABLE.code());
      grants.insert(Entities.create(grant));
      grantId = grant.getId();
    }
    preferences.updatePermission(preference.getId(), permission, scene, grantId, now());
    if (!grantId.isEmpty()) {
      preferences.incrementGrant(preference.getId(), now());
    }
    Data result = view.data(preferences.select(openid, TEMPLATE_KEY, role, false));
    result.put("grantId", grantId);
    return result;
  }

  /** 下单授权必须在 SQL 中同时满足本人、角色、场景、模板、状态和未绑定条件。 */
  public String bind(String openid, String grantId, String orderId) {
    if (grantId.isEmpty()) {
      return "";
    }
    require(grants.bindOrder(grantId, openid, TEMPLATE_KEY, orderId, now()) == 1, "本单授权无效或已经绑定其他订单");
    return grantId;
  }

  /** 构造尚未持久化的通知 DTO，只有 create 的白名单会进入实体。 */
  public Data notice(String recipient, String role, String category, String type, String summary, String status,
      String bizType, String bizId, String path, String dedupe) {
    if (NotificationCategory.MERCHANT_APPLICATION.matches(category)) {
      status = labels.label(DictionaryType.APPLICATION_STATUS.code(), status);
    }
    return of("recipientOpenid", recipient, "recipientRole", role, "category", category, "type", type,
        "title", labels.label(DictionaryType.NOTIFICATION_TYPE.code(), type), "summary", summary, "statusText", status,
        "bizType", bizType, "bizId", bizId, "pagePath", path, "dedupeKey", dedupe, "priority", NotificationPriority.HIGH.code());
  }

  /** 站内信按接收人和去重键幂等；微信调用延迟到业务事务成功提交之后。 */
  public Data create(Data input, boolean wechat) {
    String recipient = input.required("recipientOpenid", 64);
    String dedupe = input.required("dedupeKey", 255);
    Data value = input.pick("recipientOpenid", "recipientRole", "subscriptionRole", "category", "type",
        "priority", "title", "summary", "statusText", "bizType", "bizId", "pagePath",
        "subscriptionGrantId", "requireSpecificGrant", "dedupeKey");
    String path = value.text("pagePath", MESSAGES_PAGE);
    value.with("pagePath", path + (path.contains("?") ? "&" : "?") + BACKEND_QUERY);
    value.putIfAbsent("subscriptionRole",
        UserRole.MERCHANT_OWNER.matches(value.text("recipientRole"))
            ? SubscriptionRole.MERCHANT.code()
            : SubscriptionRole.BUYER.code());
    Notification notification = Entities.copy(value, Notification.class);
    notification.setReadStatus(NotificationReadStatus.UNREAD.code());
    notification.setHidden(false);
    notification.setReadAt(0);
    notification.setExpireAt(now() + NOTIFICATION_EXPIRE_DAYS * ONE_DAY_MILLIS);
    Entities.create(notification);
    int inserted = notifications.insertIgnore(notification);
    final Notification saved = inserted == 0 ? notifications.selectByDedupe(recipient, dedupe) : notification;
    require(saved != null, "通知保存失败");
    if (inserted == 0) {
      return view.data(saved).with("wechatSent", false);
    }
    addDelivery(saved, NotificationChannel.IN_APP.code(), DeliveryStatus.SUCCESS.code());
    Data result = view.data(saved).with("wechatSent", false);
    if (wechat) {
      addDelivery(saved, NotificationChannel.WECHAT_SUBSCRIBE.code(), DeliveryStatus.PENDING.code());
      // afterCommit 之外部调用失败不能回滚已经提交的订单或审核业务。
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          try {
            result.put("wechatSent", delivery.send(saved.getId()));
          } catch (RuntimeException error) {
            LOG.error("微信通知处理失败，待诊断记录保留：{}", saved.getId(), error);
          }
        }
      });
    }
    return result;
  }

  private void addDelivery(Notification notification, String channel, String state) {
    NotificationDelivery delivery = new NotificationDelivery();
    delivery.setNotificationId(notification.getId());
    delivery.setRecipientOpenid(notification.getRecipientOpenid());
    delivery.setRecipientRole(notification.getRecipientRole());
    delivery.setSubscriptionRole(notification.getSubscriptionRole());
    delivery.setChannel(channel);
    delivery.setTemplateKey(TEMPLATE_KEY);
    delivery.setSubscriptionGrantId(notification.getSubscriptionGrantId());
    delivery.setDeliveryStatus(state);
    delivery.setAttemptCount(DeliveryStatus.SUCCESS.matches(state) ? 1 : 0);
    delivery.setSentAt(DeliveryStatus.SUCCESS.matches(state) ? now() : 0);
    deliveries.insert(Entities.create(delivery));
  }

  /** 平台通知按主键游标分页，并叠加接收人去重键。 */
  public void platform(Data base, String... roles) {
    String after = "";
    while (true) {
      List<PlatformUser> users = platformUsers.selectActiveAfter(after, MAX_LIST_SIZE);
      if (users.isEmpty()) {
        return;
      }
      for (PlatformUser user : users) {
        if (oneOf(user.getRole(), roles)) {
          create(new Data(base).with("recipientOpenid", user.getOpenid(), "recipientRole", user.getRole(),
              "subscriptionRole", SubscriptionRole.PLATFORM.code(),
              "dedupeKey", base.text("dedupeKey") + ":" + user.getOpenid()), false);
        }
      }
      after = users.get(users.size() - 1).getId();
    }
  }

  /** 返回当前用户消息列表，支持分类和未读筛选，最多 100 条。 */
  public List<Data> list(Data e, String openid) {
    return view.list(notifications.selectList(openid, e.text("category"), e.flag("unreadOnly"), MAX_LIST_SIZE));
  }

  /** 直接聚合全部未读记录，总数不会被消息列表的 100 条上限截断。 */
  public Data unread(String openid) {
    Data categories = new Data();
    long total = 0;
    for (String category : Arrays.asList(codes(NotificationCategory.ORDER,
        NotificationCategory.MERCHANT_APPLICATION, NotificationCategory.STORE,
        NotificationCategory.PLATFORM, NotificationCategory.SYSTEM))) {
      long count = notifications.countUnread(openid, category);
      if (count > 0) {
        categories.put(category, count);
      }
      total += count;
    }
    return of("total", total, "categories", categories);
  }

  /** 标记本人单条或全部消息已读，不能修改其他接收人的记录。 */
  public boolean read(String openid, String id, boolean all) {
    if (all) {
      notifications.markAllRead(openid, now(), now());
    } else {
      Notification notification = notifications.selectById(id, true);
      require(notification != null, "记录不存在");
      require(openid.equals(notification.getRecipientOpenid()), "无权操作该消息");
      notifications.markRead(id, openid, now(), now());
    }
    return true;
  }

  /** 返回本人订阅偏好和最近微信投递诊断，不包含其他用户数据。 */
  public Data diagnostics(String openid) {
    List<NotificationPreference> prefs = preferences.selectByOpenid(openid);
    NotificationPreference preferred = prefs.stream()
        .filter(p -> SubscriptionRole.PLATFORM.matches(p.getSubscriptionRole()))
        .findFirst()
        .orElse(prefs.isEmpty() ? null : prefs.get(0));
    Data config = view.data(config());
    return of("config", config.pick("templateId", "enabled", "miniprogramState", "fields",
        "buyerOrderPushStage", "buyerOrderPushStageLabel"),
        "preferences", view.list(prefs), "preference", view.data(preferred),
        "latestDeliveries", view.list(deliveries.selectLatest(openid, NotificationChannel.WECHAT_SUBSCRIBE.code(), RECENT_DELIVERY_LIMIT)));
  }

  /** 创建本人测试通知；只能领取非下单场景且未绑定业务的授权。 */
  public Data test(Data e, String openid) {
    String role = e.text("subscriptionRole", SubscriptionRole.GENERAL.code());
    require(oneOf(role, codes(SubscriptionRole.BUYER, SubscriptionRole.MERCHANT,
        SubscriptionRole.APPLICANT, SubscriptionRole.PLATFORM, SubscriptionRole.GENERAL)), "订阅身份不正确");
    String id = id();
    return create(notice(openid, UserRole.CUSTOMER.code(), NotificationCategory.SYSTEM.code(),
        NotificationType.TEST_NOTIFICATION.code(), "统一订阅消息测试", "测试成功",
        BizType.SYSTEM.code(), id, MESSAGES_PAGE, DedupeEvent.TEST.code() + ":" + id)
        .with("subscriptionRole", role, "requireSpecificGrant", false), true);
  }
}
