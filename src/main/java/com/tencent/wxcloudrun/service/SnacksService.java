package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.util.function.BiFunction;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 每个 action 一个数据库事务；微信发送使用提交后独立事务。 */
@Service
public class SnacksService {
  private final IdentityService identity;
  private final Map<String, BiFunction<Data, String, Object>> actions = new LinkedHashMap<>();

  /** 显式注册 45 个 action，避免根据客户端字符串反射调用任意服务方法。 */
  public SnacksService(IdentityService identity, ApplicationService applications, CatalogService catalog,
      OrderService orders, PlatformService platform, NotificationService notices) {
    this.identity = identity;
    register(Action.INIT_COLLECTIONS, (e, u) -> true);
    register(Action.GET_MY_IDENTITY, (e, u) -> identity.identity(u, identity.ensure(u)));
    register(Action.SUBMIT_MERCHANT_APPLICATION, applications::submit);
    register(Action.GET_MY_MERCHANT_APPLICATION,
        (e, u) -> identity.publicApplication(identity.latestApplication(u)));
    register(Action.LIST_MERCHANT_APPLICATIONS, applications::list);
    register(Action.GET_MERCHANT_APPLICATION_DETAIL, applications::detail);
    for (Action action : Arrays.asList(Action.MARK_APPLICATION_CONTACTED, Action.MARK_APPLICATION_CONTACT_FAILED,
        Action.REQUEST_MORE_INFORMATION, Action.SET_PAYMENT_PENDING, Action.CONFIRM_OFFLINE_PAYMENT,
        Action.APPROVE_MERCHANT_APPLICATION, Action.REJECT_MERCHANT_APPLICATION)) {
      register(action, (e, u) -> applications.operate(action, e, u));
    }
    register(Action.LIST_STORES, (e, u) -> catalog.stores(e));
    register(Action.GET_STORE, (e, u) -> catalog.store(e.required("storeId", 64)));
    register(Action.GET_MY_STORE, (e, u) -> identity.storeView(identity.owned(u)));
    register(Action.SAVE_STORE, catalog::saveStore);
    register(Action.SET_STORE_OPEN, catalog::open);
    register(Action.LIST_PRODUCTS, catalog::products);
    register(Action.GET_PRODUCT, catalog::product);
    register(Action.SAVE_PRODUCT, catalog::saveProduct);
    register(Action.TOGGLE_PRODUCT, catalog::toggle);
    register(Action.CREATE_ORDER, orders::create);
    register(Action.LIST_ORDERS, orders::list);
    register(Action.GET_ORDER, (e, u) -> orders.get(e.required("orderId", 64), u));
    register(Action.UPDATE_ORDER_STATUS, orders::update);
    register(Action.GET_MERCHANT_STATS, (e, u) -> catalog.stats(u));
    register(Action.GET_ADMIN_DASHBOARD, (e, u) -> platform.dashboard(u));
    register(Action.LIST_ADMIN_STORES, (e, u) -> platform.stores(u));
    register(Action.SET_STORE_PLATFORM_STATUS, platform::storeStatus);
    register(Action.LIST_PLATFORM_USERS, (e, u) -> platform.team(u));
    register(Action.SAVE_PLATFORM_USER, platform::saveUser);
    register(Action.GET_NOTIFICATION_CONFIG, (e, u) -> notices.publicConfig());
    register(Action.GET_NOTIFICATION_ADMIN_CONFIG, (e, u) -> {
      identity.requirePlatform(u, codes(PlatformRole.ADMIN, PlatformRole.SUPER));
      return notices.adminConfig();
    });
    register(Action.SAVE_NOTIFICATION_TEMPLATE, (e, u) -> notices.saveConfig(e.object("template"), u));
    register(Action.RECORD_SUBSCRIBE_PERMISSION, notices::permission);
    register(Action.SEND_TEST_NOTIFICATION, notices::test);
    register(Action.GET_MY_NOTIFICATION_DIAGNOSTICS, (e, u) -> notices.diagnostics(u));
    register(Action.LIST_NOTIFICATIONS, notices::list);
    register(Action.GET_UNREAD_NOTIFICATION_COUNT, (e, u) -> notices.unread(u));
    register(Action.MARK_NOTIFICATION_READ,
        (e, u) -> notices.read(u, e.required("notificationId", 64), false));
    register(Action.MARK_ALL_NOTIFICATIONS_READ, (e, u) -> notices.read(u, "", true));
    for (Action action : Arrays.asList(Action.LIST_SYSTEM_DICTIONARIES, Action.LIST_COLLECTION_SCHEMAS,
        Action.SYNC_CHINESE_DESCRIPTIONS)) {
      register(action, (e, u) -> platform.dictionaries(action, u));
    }
  }

  /** 以枚举注册处理器，Map 仍使用原协议字符串，保证小程序调用不变。 */
  private void register(Action action, BiFunction<Data, String, Object> handler) {
    actions.put(action.code(), handler);
  }

  /** 返回协议支持的 action 集合，供契约测试及运维核对。 */
  public Set<String> supportedActions() {
    return Collections.unmodifiableSet(actions.keySet());
  }

  /** 统一身份确认和 READ_COMMITTED 事务入口；网关 OpenID 不接受请求体覆盖。 */
  @Transactional(isolation = Isolation.READ_COMMITTED, timeout = TRANSACTION_TIMEOUT_SECONDS)
  public Object execute(Data event, String openid) {
    BiFunction<Data, String, Object> handler = actions.get(event.text("action"));
    if (handler == null) {
      throw new BusinessException("未知操作", ErrorCode.UNKNOWN_ACTION.code(), HTTP_STATUS_OK);
    }
    if (!Action.INIT_COLLECTIONS.matches(event.text("action"))) {
      identity.ensure(openid);
    }
    return handler.apply(event, openid);
  }
}
