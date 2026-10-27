package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 店铺、商品查询和商家商品资料维护。 */
@Service
public class CatalogService {
  private final StoreMapper stores;
  private final ProductMapper products;
  private final OrderMapper orders;
  private final IdentityService identity;
  private final EntityView view;

  public CatalogService(StoreMapper stores, ProductMapper products, OrderMapper orders,
      IdentityService identity, EntityView view) {
    this.stores = stores;
    this.products = products;
    this.orders = orders;
    this.identity = identity;
    this.view = view;
  }

  /** 店铺列表查询条件由固定 Mapper 参数表达，不接受动态列名。 */
  public List<Data> stores(Data e) {
    return view.list(stores.selectPublic(e.text("keyword"), MAX_LIST_SIZE));
  }

  /** 返回已审核且平台有效的店铺详情及在售商品。 */
  public Data store(String id) {
    Store store = stores.selectById(id, false);
    require(visible(store), "店铺不存在或已暂停");
    return view.data(store).with("products", view.list(products.selectByStore(id, ProductStatus.ON.code(), MAX_LIST_SIZE)));
  }

  /** 商家仅修改白名单资料；评分、销量和审核字段仍由服务端维护。 */
  public Data saveStore(Data e, String openid) {
    Data input = e.object("store");
    String id = input.text("id", input.text("_id"));
    Store store = identity.owned(openid, id, true);
    Store value = Entities.copy(storeFields(input, false), Store.class);
    value.setId(store.getId());
    value.setOwnerOpenid(openid);
    require(stores.updateProfile(Entities.update(value)) == 1, "店铺资料保存失败");
    return view.data(stores.selectById(store.getId(), false));
  }

  /** 营业开关只影响 businessStatus，不绕过审核和平台停权。 */
  public boolean open(Data e, String openid) {
    Data input = e.object("store");
    String id = input.text("id", input.text("_id"));
    Store store = identity.owned(openid, id, true);
    boolean open = input.flag("open");
    require(stores.updateBusinessStatus(store.getId(), open ? StoreBusinessStatus.OPEN.code() : StoreBusinessStatus.CLOSED.code(), now()) == 1, "营业状态保存失败");
    return true;
  }

  /** 返回店铺商品；商家管理视图包含下架商品。 */
  public List<Data> products(Data e, String openid) {
    Store store = identity.owned(openid);
    require(store != null, "你还没有店铺");
    return view.list(products.selectByStore(store.getId(), null, MAX_LIST_SIZE));
  }

  /** 返回商家自己的商品详情。 */
  public Data product(Data e, String openid) {
    Store store = identity.owned(openid);
    Product product = products.selectById(e.required("productId", 64), false);
    require(store != null && product != null && store.getId().equals(product.getStoreId()), "商品不存在");
    return view.data(product);
  }

  /** 新增商品由服务端生成 ID；编辑使用版本号防止旧资料覆盖新库存。 */
  public Data saveProduct(Data e, String openid) {
    Data input = e.object("product");
    Store store = identity.owned(openid);
    require(store != null, "你还没有店铺");
    Product value = Entities.copy(productFields(input), Product.class);
    String id = input.text("id", input.text("_id"));
    if (id.isEmpty()) {
      value.setStoreId(store.getId());
      value.setStatus(ProductStatus.ON.code());
      value.setSold(0);
      products.insert(Entities.create(value));
      return view.data(value);
    }
    Product old = products.selectById(id, true);
    require(old != null && store.getId().equals(old.getStoreId()), "商品不存在");
    value.setId(id);
    value.setStoreId(old.getStoreId());
    value.setStatus(old.getStatus());
    require(products.updateDetails(Entities.update(value), input.number("version")) == 1, "商品已变化，请刷新后重试");
    return view.data(products.selectById(id, false));
  }

  /** 上下架与资料更新都检查当前归属，不能操作其他店铺商品。 */
  public boolean toggle(Data e, String openid) {
    Data input = e.object("product");
    Store store = identity.owned(openid);
    require(store != null, "你还没有店铺");
    String id = input.text("id", input.text("_id"));
    Product old = products.selectById(id, true);
    require(old != null && store.getId().equals(old.getStoreId()), "商品不存在");
    String status = input.text("status");
    require(oneOf(status, codes(ProductStatus.ON, ProductStatus.OFF)), "商品状态不正确");
    require(products.updateStatus(id, status, input.number("version"), now()) == 1, "商品已变化，请刷新后重试");
    return true;
  }

  /** 商家统计只聚合本店订单，平台统计另有独立入口。 */
  public Data stats(String openid) {
    Store store = identity.owned(openid);
    require(store != null, "你还没有店铺");
    long since = now() - ONE_DAY_MILLIS;
    return of("todayOrders", orders.countByStoreSince(store.getId(), since),
        "todayAmount", orders.sumCompletedSince(store.getId(), since),
        "pendingOrders", orders.countByStoreAndStatus(store.getId(), OrderStatus.CREATED.code()),
        "lowStock", products.countLowStock(store.getId(), LOW_STOCK_THRESHOLD));
  }

  /** 只有审核通过且未被平台暂停的店铺可公开访问或下单。 */
  public static boolean visible(Store store) {
    return store != null && StoreApprovalStatus.APPROVED.matches(store.getApprovalStatus())
        && StorePlatformStatus.ACTIVE.matches(store.getPlatformStatus());
  }

  /** 校验店铺资料白名单、字符串长度和非负金额。 */
  public static Data storeFields(Data in, boolean applying) {
    Data out = new Data();
    text(out, in, "storeName", 100);
    text(out, in, "name", 100);
    text(out, in, "ownerName", 50);
    text(out, in, "phone", 50);
    if (in.containsKey("phone")) {
      require(in.text("phone").matches(PHONE_PATTERN), "请输入正确手机号");
    }
    text(out, in, "notice", 500);
    text(out, in, "businessHours", 200);
    if (applying) {
      text(out, in, "serviceDescription", 1000);
      text(out, in, "wechatContact", 100);
    }
    money(out, in, "minimumOrder", 0, MAX_CATALOG_NUMBER);
    money(out, in, "deliveryFee", 0, MAX_CATALOG_NUMBER);
    integer(out, in, "deliveryMinutes", 0, MAX_DELIVERY_MINUTES);
    return out;
  }

  /** 商品图片继续保存原云存储 fileID，不迁移二进制内容。 */
  static Data productFields(Data in) {
    Data out = new Data();
    text(out, in, "name", 100);
    text(out, in, "emoji", 100);
    money(out, in, "price", 0, MAX_CATALOG_NUMBER);
    integer(out, in, "stock", 0, MAX_CATALOG_NUMBER);
    text(out, in, "image", 1000);
    if (in.get("image") == null) {
      text(out, in, "imageFileId", 1000);
    }
    if (in.get("image") == null && in.get("imageFileId") == null) {
      out.put("image", "");
    }
    text(out, in, "category", 100);
    return out;
  }

  static void text(Data out, Data in, String key, int max) {
    text(out, in, key, key, max);
  }

  static void text(Data out, Data in, String key, String target, int max) {
    if (in.containsKey(key)) {
      String value = in.text(key);
      require(value.length() <= max, key + " 长度不能超过 " + max);
      out.put(target, value);
    }
  }

  static void money(Data out, Data in, String key, long min, long max) {
    integer(out, in, key, min, max);
  }

  static void integer(Data out, Data in, String key, long min, long max) {
    if (in.containsKey(key)) {
      out.put(key, in.integer(key, 0, min, max));
    }
  }

  static long now() {
    return System.currentTimeMillis();
  }

  static void require(boolean valid, String message) {
    if (!valid) {
      throw new BusinessException(message);
    }
  }

  static boolean oneOf(String value, String... choices) {
    for (String choice : choices) {
      if (Objects.equals(value, choice)) {
        return true;
      }
    }
    return false;
  }
}
