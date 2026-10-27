package com.tencent.wxcloudrun.constant;

/**
 * 宿舍零食业务统一定义。
 *
 * <p>所有枚举的 code 都是数据库和 HTTP 协议实际使用的值，禁止直接修改已发布的 code。
 * 无明确枚举集合的系统固定值在本类中统一定义；异常提示和自描述字段名保留在调用处。</p>
 */
public final class SnacksConstants {
  private SnacksConstants() {
  }

  /** 带字符串协议编码的业务枚举。 */
  public interface Coded {
    String code();

    /** 判断数据库或请求中的原始值是否等于当前枚举。 */
    default boolean matches(String value) {
      return code().equals(value);
    }
  }

  /** 将一组业务枚举转换成现有服务需要的字符串数组。 */
  public static String[] codes(Coded... values) {
    String[] result = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = values[i].code();
    }
    return result;
  }

  /** 平台管理角色；与普通小程序用户角色分开授权。 */
  public enum PlatformRole implements Coded {
    /** 运营人员，可处理申请、审核和店铺治理。 */
    OPERATOR("PLATFORM_OPERATOR"),
    /** 平台管理员，可维护运营人员。 */
    ADMIN("PLATFORM_ADMIN"),
    /** 超级管理员，拥有平台团队维护权限。 */
    SUPER("SUPER_ADMIN");

    private final String code;

    PlatformRole(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** 判断是否为平台角色，避免通过字符串片段猜测权限。 */
    public static boolean isPlatformRole(String value) {
      return OPERATOR.matches(value) || ADMIN.matches(value) || SUPER.matches(value);
    }
  }

  /** 小程序用户角色。 */
  public enum UserRole implements Coded {
    /** 普通购买者。 */
    CUSTOMER("CUSTOMER"),
    /** 已审核通过的店铺经营者。 */
    MERCHANT_OWNER("MERCHANT_OWNER");

    private final String code;

    UserRole(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 用户或平台成员的账号状态。 */
  public enum AccountStatus implements Coded {
    /** 正常可用。 */
    ACTIVE("ACTIVE"),
    /** 已停用，不允许继续以该身份操作。 */
    DISABLED("DISABLED");

    private final String code;

    AccountStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 商家入驻申请的完整生命周期状态。 */
  public enum ApplicationStatus implements Coded {
    /** 草稿，尚未提交审核。 */
    DRAFT("DRAFT"),
    /** 已提交，等待平台联系。 */
    SUBMITTED("SUBMITTED"),
    /** 平台已联系申请人。 */
    CONTACTED("CONTACTED"),
    /** 联系失败，可修改资料后重新提交。 */
    CONTACT_FAILED("CONTACT_FAILED"),
    /** 需要申请人补充资料。 */
    NEED_MORE_INFO("NEED_MORE_INFO"),
    /** 等待线下缴费确认。 */
    PAYMENT_PENDING("PAYMENT_PENDING"),
    /** 平台已确认线下缴费。 */
    PAID("PAID"),
    /** 审核通过并建立店铺。 */
    APPROVED("APPROVED"),
    /** 审核拒绝，可修改后重新提交。 */
    REJECTED("REJECTED"),
    /** 申请对应业务已暂停，仅用于兼容性判断。 */
    SUSPENDED("SUSPENDED");

    private final String code;

    ApplicationStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 商家入驻线下付款状态。 */
  public enum PaymentStatus implements Coded {
    /** 平台已人工确认到账。 */
    CONFIRMED("CONFIRMED");

    private final String code;

    PaymentStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 入驻缴费方式；本平台只记录线下确认，不代替支付机构记账。 */
  public enum PaymentMethod implements Coded {
    /** 线下微信转账。 */
    WECHAT_OFFLINE("WECHAT_OFFLINE"),
    /** 线下支付宝转账。 */
    ALIPAY_OFFLINE("ALIPAY_OFFLINE"),
    /** 现金。 */
    CASH("CASH"),
    /** 其他经平台确认的方式。 */
    OTHER("OTHER");

    private final String code;

    PaymentMethod(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 店铺准入审核状态。 */
  public enum StoreApprovalStatus implements Coded {
    /** 店铺已通过平台审核。 */
    APPROVED("APPROVED");

    private final String code;

    StoreApprovalStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 平台对店铺的治理状态。 */
  public enum StorePlatformStatus implements Coded {
    /** 平台允许店铺正常经营。 */
    ACTIVE("ACTIVE"),
    /** 店铺被平台暂停，不对购买者开放。 */
    SUSPENDED("SUSPENDED");

    private final String code;

    StorePlatformStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 商家自行控制的营业状态。 */
  public enum StoreBusinessStatus implements Coded {
    /** 正在营业。 */
    OPEN("OPEN"),
    /** 暂停营业。 */
    CLOSED("CLOSED");

    private final String code;

    StoreBusinessStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 商品上下架状态。 */
  public enum ProductStatus implements Coded {
    /** 已上架，可在营业店铺中购买。 */
    ON("ON"),
    /** 已下架，不允许新增购买。 */
    OFF("OFF");

    private final String code;

    ProductStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 订单状态；状态迁移和副作用必须位于同一事务。 */
  public enum OrderStatus implements Coded {
    /** 订单已创建，等待商家接单。 */
    CREATED("CREATED"),
    /** 商家已接单。 */
    ACCEPTED("ACCEPTED"),
    /** 商家拒单并回滚库存。 */
    REJECTED("REJECTED"),
    /** 购买者取消并回滚库存。 */
    CANCELED("CANCELED"),
    /** 商家正在配送。 */
    DELIVERING("DELIVERING"),
    /** 商品已送达，等待双方完成。 */
    DELIVERED("DELIVERED"),
    /** 订单已完成，销量等终态统计只增加一次。 */
    COMPLETED("COMPLETED");

    private final String code;

    OrderStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 订单履约方式。 */
  public enum DeliveryMethod implements Coded {
    /** 配送到寝室门口。 */
    DORM_DOOR("送到寝室门口"),
    /** 配送到本楼层公共区域。 */
    FLOOR_COMMON_AREA("楼层公共区域交付"),
    /** 配送到宿舍一楼。 */
    FIRST_FLOOR("送到一楼"),
    /** 购买者到店自取，不产生配送费。 */
    SELF_PICKUP("到店自取");

    private final String code;

    DeliveryMethod(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 通知业务分类。 */
  public enum NotificationCategory implements Coded {
    /** 订单履约消息。 */
    ORDER("ORDER"),
    /** 商家入驻申请消息。 */
    MERCHANT_APPLICATION("MERCHANT_APPLICATION"),
    /** 店铺治理消息。 */
    STORE("STORE"),
    /** 平台工作通知。 */
    PLATFORM("PLATFORM"),
    /** 系统及测试消息。 */
    SYSTEM("SYSTEM");

    private final String code;

    NotificationCategory(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 通知关联业务对象类型。 */
  public enum BizType implements Coded {
    /** 关联订单记录。 */
    ORDER("ORDER"),
    /** 关联入驻申请记录。 */
    MERCHANT_APPLICATION("MERCHANT_APPLICATION"),
    /** 关联店铺记录。 */
    STORE("STORE"),
    /** 不关联具体业务记录的系统消息。 */
    SYSTEM("SYSTEM"),
    /** 关联平台工作事项。 */
    PLATFORM("PLATFORM");

    private final String code;

    BizType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 通知类型；名称用于站内分类、微信场景绑定和审计检索。 */
  public enum NotificationType implements Coded {
    /** 告知申请人资料已提交。 */
    APPLICATION_SUBMITTED("APPLICATION_SUBMITTED"),
    /** 告知平台出现新申请。 */
    NEW_MERCHANT_APPLICATION("NEW_MERCHANT_APPLICATION"),
    /** 告知平台申请已重新提交。 */
    APPLICATION_RESUBMITTED("APPLICATION_RESUBMITTED"),
    /** 告知申请人平台已联系。 */
    APPLICATION_CONTACTED("APPLICATION_CONTACTED"),
    /** 告知申请人平台联系失败。 */
    APPLICATION_CONTACT_FAILED("APPLICATION_CONTACT_FAILED"),
    /** 告知申请人需要补充资料。 */
    APPLICATION_NEED_MORE_INFO("APPLICATION_NEED_MORE_INFO"),
    /** 告知申请人等待缴费。 */
    APPLICATION_PAYMENT_PENDING("APPLICATION_PAYMENT_PENDING"),
    /** 告知申请人缴费已确认。 */
    APPLICATION_PAYMENT_CONFIRMED("APPLICATION_PAYMENT_CONFIRMED"),
    /** 告知平台已付款申请等待终审。 */
    APPLICATION_PAID_PENDING_APPROVAL("APPLICATION_PAID_PENDING_APPROVAL"),
    /** 告知申请人审核通过。 */
    APPLICATION_APPROVED("APPLICATION_APPROVED"),
    /** 告知申请人审核拒绝。 */
    APPLICATION_REJECTED("APPLICATION_REJECTED"),
    /** 告知商家收到新订单。 */
    NEW_ORDER("NEW_ORDER"),
    /** 告知商家购买者取消订单。 */
    BUYER_CANCELED_ORDER("BUYER_CANCELED_ORDER"),
    /** 告知购买者商家已接单。 */
    ORDER_ACCEPTED("ORDER_ACCEPTED"),
    /** 告知购买者商家已拒单。 */
    ORDER_REJECTED("ORDER_REJECTED"),
    /** 告知购买者订单正在配送。 */
    ORDER_DELIVERING("ORDER_DELIVERING"),
    /** 告知购买者订单已送达。 */
    ORDER_DELIVERED("ORDER_DELIVERED"),
    /** 告知商家店铺已恢复。 */
    STORE_RESUMED("STORE_RESUMED"),
    /** 告知商家店铺已暂停。 */
    STORE_SUSPENDED("STORE_SUSPENDED"),
    /** 用户主动发起的订阅测试。 */
    TEST_NOTIFICATION("TEST_NOTIFICATION");

    private final String code;

    NotificationType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    /** 订单推进通知类型与订单状态保持固定映射，其余状态不触发买家进度通知。 */
    public static NotificationType forOrderStatus(OrderStatus status) {
      switch (status) {
        case ACCEPTED:
          return ORDER_ACCEPTED;
        case REJECTED:
          return ORDER_REJECTED;
        case DELIVERING:
          return ORDER_DELIVERING;
        case DELIVERED:
          return ORDER_DELIVERED;
        default:
          throw new IllegalArgumentException("该订单状态不触发买家进度通知：" + status);
      }
    }
  }

  /** 站内信读取状态。 */
  public enum NotificationReadStatus implements Coded {
    /** 新通知尚未读取。 */
    UNREAD("UNREAD");

    private final String code;

    NotificationReadStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 通知发送通道。 */
  public enum NotificationChannel implements Coded {
    /** 小程序站内通知。 */
    IN_APP("IN_APP"),
    /** 微信订阅消息。 */
    WECHAT_SUBSCRIBE("WECHAT_SUBSCRIBE");

    private final String code;

    NotificationChannel(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 通知优先级。 */
  public enum NotificationPriority implements Coded {
    /** 需要优先查看。 */
    HIGH("HIGH"),
    /** 需要立即处理，例如新订单。 */
    URGENT("URGENT");

    private final String code;

    NotificationPriority(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 微信订阅消息授权状态。 */
  public enum GrantStatus implements Coded {
    /** 可用于一次真实业务事件。 */
    AVAILABLE("AVAILABLE"),
    /** 已原子领取，正在调用微信接口。 */
    SENDING("SENDING"),
    /** 已确认消费，不得再次使用。 */
    CONSUMED("CONSUMED"),
    /** 微信明确拒绝或业务校验失败。 */
    FAILED("FAILED"),
    /** 外部调用异常，结果未知，禁止自动重发。 */
    UNKNOWN("UNKNOWN");

    private final String code;

    GrantStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 单次通知投递状态。 */
  public enum DeliveryStatus implements Coded {
    /** 已创建，等待独立投递事务领取。 */
    PENDING("PENDING"),
    /** 已领取，正在调用外部通道。 */
    SENDING("SENDING"),
    /** 外部通道明确成功。 */
    SUCCESS("SUCCESS"),
    /** 外部通道明确失败，不再自动发送。 */
    FAILED_FINAL("FAILED_FINAL"),
    /** 外部结果未知，不自动重发。 */
    UNKNOWN("UNKNOWN"),
    /** 模板未开启或授权不匹配，主动跳过。 */
    SKIPPED("SKIPPED");

    private final String code;

    DeliveryStatus(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 订阅消息授权所属角色。 */
  public enum SubscriptionRole implements Coded {
    /** 购买者订单通知授权。 */
    BUYER("BUYER"),
    /** 商家订单通知授权。 */
    MERCHANT("MERCHANT"),
    /** 入驻申请人通知授权。 */
    APPLICANT("APPLICANT"),
    /** 平台工作通知授权。 */
    PLATFORM("PLATFORM"),
    /** 未指定业务身份的通用授权。 */
    GENERAL("GENERAL");

    private final String code;

    SubscriptionRole(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 微信 subscribeMessage 组件返回的订阅选择。 */
  public enum SubscribePermission implements Coded {
    /** 用户接受，生成一次性授权。 */
    ACCEPT("ACCEPT"),
    /** 用户本次拒绝。 */
    REJECT("REJECT"),
    /** 用户长期屏蔽该模板。 */
    BAN("BAN"),
    /** 模板被微信组件过滤。 */
    FILTER("FILTER"),
    /** 客户端无法确认选择结果。 */
    UNKNOWN("UNKNOWN");

    private final String code;

    SubscribePermission(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 微信跳转的小程序发布环境。 */
  public enum MiniProgramState implements Coded {
    /** 开发版。 */
    DEVELOPER("developer"),
    /** 体验版。 */
    TRIAL("trial"),
    /** 正式版。 */
    FORMAL("formal");

    private final String code;

    MiniProgramState(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 购买者订单微信推送的触发节点。 */
  public enum BuyerOrderPushStage implements Coded {
    /** 商家接单时推送。 */
    ACCEPTED("ACCEPTED"),
    /** 订单送达时推送。 */
    DELIVERED("DELIVERED");

    private final String code;

    BuyerOrderPushStage(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 与业务安全相关的微信接口结果码。 */
  public enum WechatResultCode implements Coded {
    /** 微信已接受发送请求。 */
    SUCCESS("0"),
    /** 用户未订阅或剩余授权次数不足。 */
    REFUSED("43101"),
    /** 网络异常导致结果无法确认。 */
    UNKNOWN("UNKNOWN");

    private final String code;

    WechatResultCode(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 中文字典类型，与 resources/dictionaries.json 一一对应。 */
  public enum DictionaryType implements Coded {
    /** 普通用户角色字典。 */
    USER_ROLE("USER_ROLE"),
    /** 账号状态字典。 */
    ACCOUNT_STATUS("ACCOUNT_STATUS"),
    /** 入驻申请状态字典。 */
    APPLICATION_STATUS("APPLICATION_STATUS"),
    /** 缴费状态字典。 */
    PAYMENT_STATUS("PAYMENT_STATUS"),
    /** 缴费方式字典。 */
    PAYMENT_METHOD("PAYMENT_METHOD"),
    /** 店铺审核状态字典。 */
    STORE_APPROVAL_STATUS("STORE_APPROVAL_STATUS"),
    /** 店铺平台状态字典。 */
    STORE_PLATFORM_STATUS("STORE_PLATFORM_STATUS"),
    /** 店铺营业状态字典。 */
    STORE_BUSINESS_STATUS("STORE_BUSINESS_STATUS"),
    /** 商品上下架状态字典。 */
    PRODUCT_STATUS("PRODUCT_STATUS"),
    /** 订单状态字典。 */
    ORDER_STATUS("ORDER_STATUS"),
    /** 通知分类字典。 */
    NOTIFICATION_CATEGORY("NOTIFICATION_CATEGORY"),
    /** 通知类型字典。 */
    NOTIFICATION_TYPE("NOTIFICATION_TYPE"),
    /** 通知读取状态字典。 */
    NOTIFICATION_READ_STATUS("NOTIFICATION_READ_STATUS"),
    /** 通知通道字典。 */
    NOTIFICATION_CHANNEL("NOTIFICATION_CHANNEL"),
    /** 通知优先级字典。 */
    NOTIFICATION_PRIORITY("NOTIFICATION_PRIORITY"),
    /** 订阅授权状态字典。 */
    GRANT_STATUS("GRANT_STATUS"),
    /** 通用投递状态字典。 */
    DELIVERY_STATUS("DELIVERY_STATUS"),
    /** 订阅角色字典。 */
    SUBSCRIPTION_ROLE("SUBSCRIPTION_ROLE"),
    /** 订阅选择结果字典。 */
    SUBSCRIBE_PERMISSION("SUBSCRIBE_PERMISSION"),
    /** 平台角色字典。 */
    PLATFORM_ROLE("PLATFORM_ROLE"),
    /** 审计动作字典。 */
    AUDIT_ACTION("AUDIT_ACTION"),
    /** 审计目标字典。 */
    AUDIT_TARGET_TYPE("AUDIT_TARGET_TYPE"),
    /** 通知业务对象字典。 */
    NOTIFICATION_BIZ_TYPE("NOTIFICATION_BIZ_TYPE"),
    /** 通知投递状态字典。 */
    NOTIFICATION_DELIVERY_STATUS("NOTIFICATION_DELIVERY_STATUS"),
    /** 购买者微信推送节点字典。 */
    BUYER_ORDER_PUSH_STAGE("BUYER_ORDER_PUSH_STAGE"),
    /** 通知订阅角色展示字典。 */
    NOTIFICATION_SUBSCRIPTION_ROLE("NOTIFICATION_SUBSCRIPTION_ROLE");

    private final String code;

    DictionaryType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 平台审计动作，使用固定编码便于检索和统计。 */
  public enum AuditAction implements Coded {
    /** 标记申请人联系成功。 */
    MARK_APPLICATION_CONTACTED("MARK_APPLICATION_CONTACTED"),
    /** 标记申请人联系失败。 */
    MARK_APPLICATION_CONTACT_FAILED("MARK_APPLICATION_CONTACT_FAILED"),
    /** 要求申请人补充资料。 */
    REQUEST_MORE_INFORMATION("REQUEST_MORE_INFORMATION"),
    /** 将申请推进到待缴费。 */
    SET_PAYMENT_PENDING("SET_PAYMENT_PENDING"),
    /** 人工确认线下缴费。 */
    CONFIRM_OFFLINE_PAYMENT("CONFIRM_OFFLINE_PAYMENT"),
    /** 审核通过入驻申请。 */
    APPROVE_MERCHANT_APPLICATION("APPROVE_MERCHANT_APPLICATION"),
    /** 拒绝入驻申请。 */
    REJECT_MERCHANT_APPLICATION("REJECT_MERCHANT_APPLICATION"),
    /** 平台暂停店铺。 */
    SUSPEND_STORE("SUSPEND_STORE"),
    /** 平台恢复店铺。 */
    RESUME_STORE("RESUME_STORE"),
    /** 新增或修改平台成员。 */
    SAVE_PLATFORM_USER("SAVE_PLATFORM_USER"),
    /** 主动同步中文字典和字段说明。 */
    SYNC_CHINESE_DESCRIPTIONS("SYNC_CHINESE_DESCRIPTIONS"),
    /** 保存微信订阅模板配置。 */
    SAVE_NOTIFICATION_TEMPLATE("SAVE_NOTIFICATION_TEMPLATE");

    private final String code;

    AuditAction(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 审计目标类型，与通知业务对象类型分开定义，避免误用。 */
  public enum AuditTargetType implements Coded {
    /** 入驻申请。 */
    MERCHANT_APPLICATION("MERCHANT_APPLICATION"),
    /** 店铺。 */
    STORE("STORE"),
    /** 订单。 */
    ORDER("ORDER"),
    /** 平台成员。 */
    PLATFORM_USER("PLATFORM_USER"),
    /** 系统级配置。 */
    SYSTEM("SYSTEM"),
    /** 系统字典和字段说明。 */
    SYSTEM_DICTIONARY("SYSTEM_DICTIONARY"),
    /** 通知模板。 */
    NOTIFICATION_TEMPLATE("NOTIFICATION_TEMPLATE");

    private final String code;

    AuditTargetType(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 当前用户身份响应中的权限编码，仅用于前端展示，服务端仍逐接口鉴权。 */
  public enum Permission implements Coded {
    /** 创建订单。 */
    ORDER_CREATE("ORDER_CREATE"),
    /** 查看本人订单。 */
    ORDER_VIEW_SELF("ORDER_VIEW_SELF"),
    /** 提交入驻申请。 */
    MERCHANT_APPLY("MERCHANT_APPLY"),
    /** 管理本人店铺。 */
    STORE_MANAGE_SELF("STORE_MANAGE_SELF"),
    /** 管理本人店铺商品。 */
    PRODUCT_MANAGE_SELF("PRODUCT_MANAGE_SELF"),
    /** 处理本人店铺订单。 */
    ORDER_MANAGE_STORE("ORDER_MANAGE_STORE"),
    /** 查看入驻申请列表。 */
    APPLICATION_LIST("APPLICATION_LIST"),
    /** 跟进申请联系记录。 */
    APPLICATION_CONTACT("APPLICATION_CONTACT"),
    /** 确认线下缴费。 */
    PAYMENT_CONFIRM("PAYMENT_CONFIRM"),
    /** 查看全部店铺。 */
    STORE_VIEW_ALL("STORE_VIEW_ALL"),
    /** 审核入驻申请。 */
    APPLICATION_APPROVE("APPLICATION_APPROVE"),
    /** 暂停或恢复店铺。 */
    STORE_SUSPEND("STORE_SUSPEND"),
    /** 平台商品治理。 */
    PRODUCT_MODERATE("PRODUCT_MODERATE"),
    /** 查看平台审计日志。 */
    AUDIT_LOG_VIEW("AUDIT_LOG_VIEW"),
    /** 维护平台成员。 */
    PLATFORM_USER_MANAGE("PLATFORM_USER_MANAGE"),
    /** 维护平台配置。 */
    PLATFORM_CONFIG_MANAGE("PLATFORM_CONFIG_MANAGE");

    private final String code;

    Permission(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 对外业务错误码；HTTP 状态仍由异常或异常处理器单独决定。 */
  public enum ErrorCode implements Coded {
    /** 可向用户展示的一般业务校验错误。 */
    BUSINESS_ERROR("BUSINESS_ERROR"),
    /** 请求 action 未注册。 */
    UNKNOWN_ACTION("UNKNOWN_ACTION"),
    /** 缺少可信微信身份。 */
    UNAUTHENTICATED("UNAUTHENTICATED"),
    /** 身份有效但没有目标操作权限。 */
    FORBIDDEN("FORBIDDEN"),
    /** 同一幂等键提交了不同内容。 */
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT"),
    /** 唯一键或并发状态冲突。 */
    CONFLICT("CONFLICT"),
    /** 请求 JSON 或参数格式错误。 */
    INVALID_ARGUMENT("INVALID_ARGUMENT"),
    /** 未预期服务端错误。 */
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;

    ErrorCode(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 通知去重键前缀；同一真实业务事件只生成一次站内通知。 */
  public enum DedupeEvent implements Coded {
    /** 入驻申请提交事件。 */
    APPLICATION_SUBMIT("APPLICATION_SUBMIT"),
    /** 线下缴费确认事件，沿用 PAID 协议前缀。 */
    PAYMENT_CONFIRMED("PAID"),
    /** 新订单创建事件。 */
    NEW_ORDER("NEW_ORDER"),
    /** 单次测试通知事件。 */
    TEST("TEST");

    private final String code;

    DedupeEvent(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** HTTP 协议支持的 45 个 action；code 必须与小程序调用保持一致。 */
  public enum Action implements Coded {
    /** 初始化固定系统数据。 */
    INIT_COLLECTIONS("initCollections"),
    /** 查询当前用户身份和权限。 */
    GET_MY_IDENTITY("getMyIdentity"),
    /** 提交或重新提交入驻申请。 */
    SUBMIT_MERCHANT_APPLICATION("submitMerchantApplication"),
    /** 查询本人入驻申请。 */
    GET_MY_MERCHANT_APPLICATION("getMyMerchantApplication"),
    /** 平台查询申请列表。 */
    LIST_MERCHANT_APPLICATIONS("listMerchantApplications"),
    /** 平台查询申请详情。 */
    GET_MERCHANT_APPLICATION_DETAIL("getMerchantApplicationDetail"),
    /** 标记申请联系成功。 */
    MARK_APPLICATION_CONTACTED("markApplicationContacted"),
    /** 标记申请联系失败。 */
    MARK_APPLICATION_CONTACT_FAILED("markApplicationContactFailed"),
    /** 要求申请人补充资料。 */
    REQUEST_MORE_INFORMATION("requestMoreInformation"),
    /** 设置申请为待缴费。 */
    SET_PAYMENT_PENDING("setPaymentPending"),
    /** 确认线下缴费。 */
    CONFIRM_OFFLINE_PAYMENT("confirmOfflinePayment"),
    /** 审核通过入驻申请。 */
    APPROVE_MERCHANT_APPLICATION("approveMerchantApplication"),
    /** 拒绝入驻申请。 */
    REJECT_MERCHANT_APPLICATION("rejectMerchantApplication"),
    /** 查询公开店铺列表。 */
    LIST_STORES("listStores"),
    /** 查询公开店铺详情。 */
    GET_STORE("getStore"),
    /** 查询本人店铺。 */
    GET_MY_STORE("getMyStore"),
    /** 保存本人店铺资料。 */
    SAVE_STORE("saveStore"),
    /** 设置本人店铺营业状态。 */
    SET_STORE_OPEN("setStoreOpen"),
    /** 查询本人店铺商品。 */
    LIST_PRODUCTS("listProducts"),
    /** 查询本人商品详情。 */
    GET_PRODUCT("getProduct"),
    /** 新增或编辑商品。 */
    SAVE_PRODUCT("saveProduct"),
    /** 商品上下架。 */
    TOGGLE_PRODUCT("toggleProduct"),
    /** 创建订单。 */
    CREATE_ORDER("createOrder"),
    /** 查询本人订单列表。 */
    LIST_ORDERS("listOrders"),
    /** 查询订单详情。 */
    GET_ORDER("getOrder"),
    /** 推进订单状态。 */
    UPDATE_ORDER_STATUS("updateOrderStatus"),
    /** 查询商家经营统计。 */
    GET_MERCHANT_STATS("getMerchantStats"),
    /** 查询平台工作台。 */
    GET_ADMIN_DASHBOARD("getAdminDashboard"),
    /** 查询平台店铺列表。 */
    LIST_ADMIN_STORES("listAdminStores"),
    /** 设置店铺平台状态。 */
    SET_STORE_PLATFORM_STATUS("setStorePlatformStatus"),
    /** 查询平台成员。 */
    LIST_PLATFORM_USERS("listPlatformUsers"),
    /** 保存平台成员。 */
    SAVE_PLATFORM_USER("savePlatformUser"),
    /** 查询用户可见通知配置。 */
    GET_NOTIFICATION_CONFIG("getNotificationConfig"),
    /** 查询平台通知配置。 */
    GET_NOTIFICATION_ADMIN_CONFIG("getNotificationAdminConfig"),
    /** 保存通知模板。 */
    SAVE_NOTIFICATION_TEMPLATE("saveNotificationTemplate"),
    /** 记录订阅选择并保存授权。 */
    RECORD_SUBSCRIBE_PERMISSION("recordSubscribePermission"),
    /** 发送本人测试通知。 */
    SEND_TEST_NOTIFICATION("sendTestNotification"),
    /** 查询本人通知诊断。 */
    GET_MY_NOTIFICATION_DIAGNOSTICS("getMyNotificationDiagnostics"),
    /** 查询本人消息列表。 */
    LIST_NOTIFICATIONS("listNotifications"),
    /** 查询本人未读数。 */
    GET_UNREAD_NOTIFICATION_COUNT("getUnreadNotificationCount"),
    /** 标记单条消息已读。 */
    MARK_NOTIFICATION_READ("markNotificationRead"),
    /** 标记全部消息已读。 */
    MARK_ALL_NOTIFICATIONS_READ("markAllNotificationsRead"),
    /** 查询中文字典。 */
    LIST_SYSTEM_DICTIONARIES("listSystemDictionaries"),
    /** 查询集合字段说明。 */
    LIST_COLLECTION_SCHEMAS("listCollectionSchemas"),
    /** 主动同步中文字典和字段说明。 */
    SYNC_CHINESE_DESCRIPTIONS("syncChineseDescriptions");

    private final String code;

    Action(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }
  }

  /** 业务校验异常沿用原接口的 HTTP 成功状态，具体错误由响应体 code 表达。 */
  public static final int HTTP_STATUS_OK = 200;
  /** 缺少或无法确认微信用户身份。 */
  public static final int HTTP_STATUS_UNAUTHORIZED = 401;
  /** 身份已确认但没有目标操作权限。 */
  public static final int HTTP_STATUS_FORBIDDEN = 403;
  /** 幂等、唯一键或并发状态冲突。 */
  public static final int HTTP_STATUS_CONFLICT = 409;
  /** 未预期的服务端内部错误。 */
  public static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;

  /** 单个 action 的数据库事务超时秒数。 */
  public static final int TRANSACTION_TIMEOUT_SECONDS = 20;
  /** 通用列表和后台工作台的最大返回条数。 */
  public static final int MAX_LIST_SIZE = 100;
  /** 申请经营品类最多允许的数量。 */
  public static final int MAX_APPLICATION_CATEGORIES = 20;
  /** 单个订单允许的商品种类上限。 */
  public static final int MAX_ORDER_ITEMS = 100;
  /** 单个商品的购买数量上限。 */
  public static final int MAX_ORDER_QUANTITY = 10_000;
  /** 店铺金额、商品价格和库存等输入值的上限。 */
  public static final int MAX_CATALOG_NUMBER = 100_000_000;
  /** 配送时长上限，单位为分钟。 */
  public static final int MAX_DELIVERY_MINUTES = 1_440;
  /** 入驻缴费记录允许的最大金额，单位为分。 */
  public static final int MAX_PAYMENT_AMOUNT_FEN = 100_000_000;
  /** 订单总额上限，单位为分。 */
  public static final long MAX_ORDER_AMOUNT_FEN = 1_000_000_000_000L;
  /** 商家工作台低库存阈值。 */
  public static final long LOW_STOCK_THRESHOLD = 5L;
  /** 毫秒制一天的长度，用于经营统计。 */
  public static final long ONE_DAY_MILLIS = 86_400_000L;
  /** 平台工作台最近审计条数。 */
  public static final int RECENT_AUDIT_LIMIT = 10;
  /** 个人通知诊断显示的最近投递条数。 */
  public static final int RECENT_DELIVERY_LIMIT = 10;
  /** 默认分页大小。 */
  public static final int DEFAULT_PAGE_SIZE = 20;
  /** 站内通知保留天数。 */
  public static final int NOTIFICATION_EXPIRE_DAYS = 365;
  /** 数据库默认字符串列的最大长度。 */
  public static final int MAX_STRING_LENGTH = 16_383;
  /** 外部错误详情保存到数据库前的截断长度。 */
  public static final int ERROR_DETAIL_LENGTH = 1_000;
  /** 微信标题和状态字段的最大 Unicode 码点数。 */
  public static final int WECHAT_SHORT_FIELD_LENGTH = 5;
  /** 微信备注字段的最大 Unicode 码点数。 */
  public static final int WECHAT_REMARK_LENGTH = 20;
  /** 微信接口连接超时秒数。 */
  public static final int WECHAT_CONNECT_TIMEOUT_SECONDS = 3;
  /** 微信接口读取超时秒数。 */
  public static final int WECHAT_READ_TIMEOUT_SECONDS = 8;

  /** 中国大陆手机号校验。 */
  public static final String PHONE_PATTERN = "1\\d{10}";
  /** 微信 OpenID 的允许字符和长度。 */
  public static final String OPENID_PATTERN = "[a-zA-Z0-9_-]{1,64}";
  /** 下单幂等请求编号格式。 */
  public static final String REQUEST_ID_PATTERN = "[a-zA-Z0-9_-]{8,64}";
  /** 微信订阅模板字段 key 格式。 */
  public static final String TEMPLATE_FIELD_PATTERN = "[a-zA-Z]+[0-9]+";
  /** 微信接口时间参数格式。 */
  public static final String WECHAT_TIME_PATTERN = "yyyy-MM-dd HH:mm";

  /** 当前 Java 后端独立字典和字段说明版本。 */
  public static final String SYSTEM_DATA_VERSION = "cloudrun-v1";
  /** 小程序双后端协议中的云托管标识。 */
  public static final String BACKEND_CLOUDRUN = "cloudrun";
  /** Java 业务记录 ID 前缀，与原云数据库 ID 隔离。 */
  public static final String RECORD_ID_PREFIX = "cr_";
  /** 订单列表商家视图参数。 */
  public static final String MERCHANT_ORDER_VIEW = "merchant";
  /** 通知链接中的后端隔离参数。 */
  public static final String BACKEND_QUERY = "backend=" + BACKEND_CLOUDRUN;
  /** 默认通知模板主键及业务 key。 */
  public static final String TEMPLATE_KEY = "SHARED_BUSINESS_NOTICE";
  /** 购买者本单绑定授权策略版本。 */
  public static final String NOTIFICATION_POLICY_VERSION = "BUYER_ORDER_BOUND_V2";
  /** 默认消息跳转页。 */
  public static final String MESSAGES_PAGE = "/pages/messages/index";
  /** 入驻申请跳转页。 */
  public static final String MERCHANT_APPLY_PAGE = "/pages/merchant-apply/index";
  /** 商家工作台跳转页。 */
  public static final String MERCHANT_PAGE = "/pages/merchant/index";
  /** 平台申请详情跳转页。 */
  public static final String ADMIN_APPLICATION_PAGE = "/pages/admin-application/index";
  /** 订单号前缀，后接不含连接符的随机标识。 */
  public static final String ORDER_NO_PREFIX = "CR";
  /** 下单请求摘要算法。 */
  public static final String ORDER_HASH_ALGORITHM = "SHA-256";
  /** 订单详情跳转页前缀，调用方追加订单 ID。 */
  public static final String ORDER_PAGE_PREFIX = "/pages/order/index?id=";

  /** 微信订阅消息发送接口，只在隔离网络边界内使用。 */
  public static final String WECHAT_SUBSCRIBE_URL = "http://api.weixin.qq.com/cgi-bin/message/subscribe/send";
  /** 微信订阅时间参数使用中国标准时区。 */
  public static final String CHINA_TIME_ZONE = "Asia/Shanghai";
  /** 微信语言参数。 */
  public static final String WECHAT_LANGUAGE = "zh_CN";

  /** 微信网关来源请求头。 */
  public static final String HEADER_WX_SOURCE = "X-WX-SOURCE";
  /** 微信网关小程序 AppID 请求头。 */
  public static final String HEADER_WX_APPID = "X-WX-APPID";
  /** 微信网关 OpenID 请求头。 */
  public static final String HEADER_WX_OPENID = "X-WX-OPENID";
  /** 多来源转发时禁止出现的来源 AppID 请求头。 */
  public static final String HEADER_WX_FROM_APPID = "X-WX-FROM-APPID";

  /** 字典资源顶层数组名称。 */
  public static final String DICTIONARIES_RESOURCE = "dictionaries";
  /** 字段说明资源顶层数组名称。 */
  public static final String SCHEMAS_RESOURCE = "schemas";
}
