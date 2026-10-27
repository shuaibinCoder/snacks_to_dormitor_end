package com.tencent.wxcloudrun.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 订单幂等创建、归属查询和单次状态副作用。 */
@Service
public class OrderService {
  private final OrderMapper orders;
  private final StoreMapper stores;
  private final ProductMapper products;
  private final IdentityService identity;
  private final NotificationService notices;
  private final ObjectMapper json;
  private final Labels labels;
  private final EntityView view;

  public OrderService(OrderMapper orders, StoreMapper stores, ProductMapper products, IdentityService identity,
      NotificationService notices, ObjectMapper json, Labels labels, EntityView view) {
    this.orders = orders;
    this.stores = stores;
    this.products = products;
    this.identity = identity;
    this.notices = notices;
    this.json = json;
    this.labels = labels;
    this.view = view;
  }

  /** 请求摘要只用于服务端幂等比较，任何订单响应都将其删除。 */
  private Data publicOrder(Order order) {
    if (order == null) {
      return null;
    }
    Data result = view.data(order);
    result.remove("requestHash");
    return result;
  }

  /** 买家、卖家或平台人员可读取订单，响应始终移除 requestHash。 */
  public Data get(String id, String openid) {
    Order order = orders.selectById(id, false);
    if (order != null) {
      require(openid.equals(order.getBuyerOpenid()) || openid.equals(order.getSellerOpenid())
          || identity.platform(openid) != null, "无权查看该订单");
    }
    return publicOrder(order);
  }

  /** 按买家或商家视图查询本人订单，最多返回 100 条。 */
  public List<Data> list(Data e, String openid) {
    boolean merchant = MERCHANT_ORDER_VIEW.equals(e.text("role"));
    return orders.selectByOwner(merchant ? null : openid, merchant ? openid : null, MAX_LIST_SIZE).stream()
        .map(this::publicOrder)
        .collect(Collectors.toList());
  }

  /** 服务端计价并在同一事务中创建订单、扣库存和绑定本单授权。 */
  public Data create(Data e, String openid) {
    Data in = e.object("order");
    String requestId = in.text("requestId", e.text("requestId"));
    require(requestId.matches(REQUEST_ID_PATTERN), "缺少有效下单幂等键，请重新进入结算页");
    TreeMap<String, Long> quantities = new TreeMap<>();
    List<Object> requested = in.array("items");
    require(!requested.isEmpty() && requested.size() <= MAX_ORDER_ITEMS, "购物车商品数量不正确");
    for (Object value : requested) {
      require(value instanceof Map, "商品参数不正确");
      Data item = new Data((Map<String, ?>) value);
      String id = item.required("productId", 64);
      long qty = item.integer("quantity", 0, 1, MAX_ORDER_QUANTITY);
      quantities.put(id, quantities.getOrDefault(id, 0L) + qty);
      require(quantities.get(id) <= MAX_ORDER_QUANTITY, "商品数量过多");
    }
    String phone = in.required("phone", 11);
    String method = in.required("deliveryMethod", 100);
    require(phone.matches(PHONE_PATTERN), "请输入正确手机号");
    require(oneOf(method, codes(DeliveryMethod.DORM_DOOR, DeliveryMethod.FLOOR_COMMON_AREA,
        DeliveryMethod.FIRST_FLOOR, DeliveryMethod.SELF_PICKUP)), "交付方式不正确");
    String location = in.text("location");
    require(DeliveryMethod.SELF_PICKUP.matches(method) || !location.isEmpty(), "请填写送达位置");
    require(location.length() <= 1000 && in.text("note").length() <= 1000, "地址或备注过长");
    Data input = of("storeId", in.required("storeId", 64), "contactName", in.required("contactName", 100),
        "phone", phone, "location", location, "deliveryMethod", method, "note", in.text("note"));
    String hash = hash(new Data(input).with("items", quantities,
        "buyerNotificationGrantId", in.text("buyerNotificationGrantId")));
    Order existing = orders.selectByRequest(openid, requestId, true);
    if (existing != null) {
      return matchingExisting(existing, hash);
    }

    Order order = new Order();
    order.setOrderNo(ORDER_NO_PREFIX + temporaryOrderNumber());
    order.setBuyerOpenid(openid);
    order.setStoreId(input.text("storeId"));
    order.setContactName(input.text("contactName"));
    order.setPhone(phone);
    order.setLocation(location);
    order.setDeliveryMethod(method);
    order.setNote(input.text("note"));
    order.setItems(new ArrayList<>());
    order.setStatus(OrderStatus.CREATED.code());
    order.setStatusHistory(new ArrayList<>());
    order.setRequestId(requestId);
    order.setRequestHash(hash);
    Entities.create(order);
    order.setOrderNo(ORDER_NO_PREFIX + order.getId().substring(RECORD_ID_PREFIX.length()));
    try {
      // 唯一键先预占请求；并发重复插入会在任何库存副作用之前被拦截。
      orders.insert(order);
    } catch (DuplicateKeyException duplicate) {
      Order raced = orders.selectByRequest(openid, requestId, false);
      if (raced == null) {
        throw duplicate;
      }
      return matchingExisting(raced, hash);
    }

    Store store = stores.selectById(order.getStoreId(), true);
    require(store != null, "记录不存在");
    require(CatalogService.visible(store) && StoreBusinessStatus.OPEN.matches(store.getBusinessStatus()),
        "店铺当前无法接单");
    List<Data> items = new ArrayList<>();
    List<Product> lockedProducts = new ArrayList<>();
    long amount = 0;
    // 商品 ID 已排序，所有下单事务采用相同的店铺、商品锁顺序。
    for (Map.Entry<String, Long> item : quantities.entrySet()) {
      Product product = products.selectById(item.getKey(), true);
      require(product != null, "部分商品不存在");
      long qty = item.getValue();
      require(store.getId().equals(product.getStoreId()) && ProductStatus.ON.matches(product.getStatus()),
          "部分商品已下架或不属于该店铺");
      require(product.getStock() >= qty, product.getName() + "库存不足");
      amount = Math.addExact(amount, Math.multiplyExact(product.getPrice(), qty));
      require(amount <= MAX_ORDER_AMOUNT_FEN, "订单金额过大");
      items.add(of("name", product.getName(), "price", product.getPrice(), "emoji", product.getEmoji(),
          "image", product.getImage(), "productId", product.getId(), "quantity", qty));
      lockedProducts.add(product);
    }
    require(amount >= store.getMinimumOrder(), "未达到起送金额");
    String grant = notices.bind(openid, in.text("buyerNotificationGrantId"), order.getId());
    order.setSellerOpenid(store.getOwnerOpenid());
    order.setStoreName(store.getName());
    order.setStoreLogoEmoji(store.getLogoEmoji());
    order.setItems(items);
    order.setGoodsAmount(amount);
    order.setDeliveryFee(store.getDeliveryFee());
    order.setTotalAmount(amount + store.getDeliveryFee());
    order.setBuyerNotificationGrantId(grant);
    order.setBuyerNotificationAuthorized(!grant.isEmpty());
    order.setStatusHistory(Collections.singletonList(history(OrderStatus.CREATED.code())));
    require(orders.completeCreation(Entities.update(order)) == 1, "订单创建冲突，请重试");
    for (Product product : lockedProducts) {
      long quantity = quantities.get(product.getId());
      require(products.deductStock(product.getId(), product.getVersion(), quantity, now()) == 1,
          product.getName() + "库存不足或已变化");
    }
    notices.create(notices.notice(store.getOwnerOpenid(), UserRole.MERCHANT_OWNER.code(),
        NotificationCategory.ORDER.code(), NotificationType.NEW_ORDER.code(),
        "收到新订单，请及时接单。", "待接单", BizType.ORDER.code(), order.getId(),
        ORDER_PAGE_PREFIX + order.getId() + "&mode=merchant", DedupeEvent.NEW_ORDER.code() + ":" + order.getId())
        .with("priority", NotificationPriority.URGENT.code()), false);
    return publicOrder(orders.selectById(order.getId(), false));
  }

  private String temporaryOrderNumber() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private Data matchingExisting(Order existing, String hash) {
    if (!hash.equals(existing.getRequestHash())) {
      throw new BusinessException("该下单请求内容已变化，请重新进入结算页",
          ErrorCode.IDEMPOTENCY_CONFLICT.code(), HTTP_STATUS_CONFLICT);
    }
    return publicOrder(existing);
  }

  /** 状态迁移先受原状态约束，再执行库存/销量副作用；任一步失败都会回滚。 */
  public Data update(Data e, String openid) {
    Order initial = orders.selectById(e.required("orderId", 64), false);
    require(initial != null, "记录不存在");
    Store store = stores.selectById(initial.getStoreId(), true);
    require(store != null, "记录不存在");
    Order order = orders.selectById(initial.getId(), true);
    require(order != null, "记录不存在");
    boolean seller = openid.equals(order.getSellerOpenid());
    boolean buyer = openid.equals(order.getBuyerOpenid());
    require(seller || buyer, "无权修改该订单");
    String old = order.getStatus();
    String target = e.text("status");
    require(allowed(old, target, seller, buyer), "当前订单状态不允许此操作");
    List<Data> history = order.getStatusHistory() == null ? new ArrayList<>() : new ArrayList<>(order.getStatusHistory());
    history.add(history(target));
    order.setStatus(target);
    order.setStatusHistory(history);
    require(orders.transitionStatus(order.getId(), old, Entities.update(order)) == 1, "订单状态已变化，请刷新后重试");
    if (oneOf(target, codes(OrderStatus.CANCELED, OrderStatus.REJECTED, OrderStatus.COMPLETED))) {
      List<Data> items = order.getItems().stream()
          .sorted(Comparator.comparing(v -> v.text("productId")))
          .collect(Collectors.toList());
      for (Data item : items) {
        Product product = products.selectById(item.text("productId"), true);
        require(product != null && store.getId().equals(product.getStoreId()), "订单商品不存在");
        long quantity = item.number("quantity");
        int changed = OrderStatus.COMPLETED.matches(target)
            ? products.increaseSold(product.getId(), quantity, now())
            : products.restoreStock(product.getId(), quantity, now());
        require(changed == 1, "订单库存或销量更新失败");
      }
      if (OrderStatus.COMPLETED.matches(target)) {
        require(stores.incrementMonthlySales(store.getId(), now()) == 1, "店铺销量更新失败");
      }
    }
    if (!OrderStatus.COMPLETED.matches(target)) {
      boolean toMerchant = OrderStatus.CANCELED.matches(target);
      String type = toMerchant ? NotificationType.BUYER_CANCELED_ORDER.code()
          : NotificationType.forOrderStatus(OrderStatus.valueOf(target)).code();
      String status = labels.label(DictionaryType.ORDER_STATUS.code(), target);
      Data notice = notices.notice(toMerchant ? order.getSellerOpenid() : order.getBuyerOpenid(),
          toMerchant ? UserRole.MERCHANT_OWNER.code() : UserRole.CUSTOMER.code(),
          NotificationCategory.ORDER.code(), type, store.getName() + "：" + status,
          status, BizType.ORDER.code(), order.getId(),
          ORDER_PAGE_PREFIX + order.getId() + (toMerchant ? "&mode=merchant" : ""), type + ":" + order.getId())
          .with("subscriptionRole",
              toMerchant ? SubscriptionRole.MERCHANT.code() : SubscriptionRole.BUYER.code(),
              "subscriptionGrantId", toMerchant ? "" : order.getBuyerNotificationGrantId(),
              "requireSpecificGrant", !toMerchant);
      notices.create(notice, !toMerchant && target.equals(notices.config().getBuyerOrderPushStage()));
    }
    return publicOrder(orders.selectById(order.getId(), false));
  }

  /** 买卖双方各自允许的状态边，终态不能再次产生库存或销量副作用。 */
  public static boolean allowed(String old, String target, boolean seller, boolean buyer) {
    if (seller) {
      return OrderStatus.CREATED.matches(old) && oneOf(target, codes(OrderStatus.ACCEPTED, OrderStatus.REJECTED))
          || OrderStatus.ACCEPTED.matches(old) && OrderStatus.DELIVERING.matches(target)
          || OrderStatus.DELIVERING.matches(old) && OrderStatus.DELIVERED.matches(target)
          || OrderStatus.DELIVERED.matches(old) && OrderStatus.COMPLETED.matches(target);
    }
    return buyer && (OrderStatus.CREATED.matches(old) && OrderStatus.CANCELED.matches(target)
        || OrderStatus.DELIVERED.matches(old) && OrderStatus.COMPLETED.matches(target));
  }

  private Data history(String status) {
    return of("status", status, "statusLabel", labels.label(DictionaryType.ORDER_STATUS.code(), status), "time", now());
  }

  /** 对排序后的商品数量及交付信息计算 SHA-256，检测幂等键被不同内容复用。 */
  private String hash(Data input) {
    try {
      byte[] digest = MessageDigest.getInstance(ORDER_HASH_ALGORITHM).digest(json.writeValueAsBytes(input));
      StringBuilder value = new StringBuilder();
      for (byte b : digest) {
        value.append(String.format("%02x", b & 255));
      }
      return value.toString();
    } catch (Exception error) {
      throw new IllegalStateException("计算下单请求摘要失败", error);
    }
  }
}
