package com.tencent.wxcloudrun;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import com.tencent.wxcloudrun.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 只连接本测试启动的 MySQL 8 容器；普通 mvn test 不执行此类。 */
@SpringBootTest(properties = {"snacks.wechat.appid=wx-test", "server.port=0"})
@Testcontainers
class SnacksMysqlIT {
  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
      .withDatabaseName("snacks_cloudrun_test").withUsername("test").withPassword("test")
      .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", MYSQL::getJdbcUrl);
    r.add("spring.datasource.username", MYSQL::getUsername);
    r.add("spring.datasource.password", MYSQL::getPassword);
    // 只为测试创建的隔离容器显式开启迁移和种子数据，应用默认仍全部关闭。
    r.add("spring.flyway.enabled", () -> true);
    r.add("snacks.initialize-on-startup", () -> true);
  }

  @Autowired SnacksService api;
  @Autowired ProductMapper products;
  @Autowired OrderMapper orders;
  @Autowired StoreMapper stores;
  @Autowired UserMapper users;
  @Autowired PlatformUserMapper platformUsers;
  @Autowired NotificationTemplateMapper templates;
  @Autowired NotificationGrantMapper grants;
  @Autowired NotificationMapper notifications;
  @Autowired NotificationDeliveryMapper deliveries;
  @Autowired PlatformTransactionManager manager;
  @Autowired NotificationService notices;
  @Autowired NotificationDeliveryService delivery;
  @Autowired SystemDataService system;
  @Autowired JdbcTemplate jdbc;
  @MockBean WechatMessageClient wechat;
  private String admin;
  private String seller;
  private String buyer;
  private String storeId;
  private String productId;
  private Data application;
  private TransactionTemplate tx;

  private Object call(String action, String user, Data input) {
    return api.execute(new Data(input).with("action", action), user);
  }

  private Data object(String action, String user, Data input) {
    return (Data) call(action, user, input);
  }

  private Data order(String request, long quantity) {
    return of("requestId", request, "storeId", storeId,
        "items", Arrays.asList(of("productId", productId, "quantity", quantity)),
        "contactName", "测试同学", "phone", "13800000000", "location", "1号楼101",
        "deliveryMethod", "送到寝室门口", "note", "测试订单");
  }

  private Data create(String user, String key, long qty) {
    return object("createOrder", user, of("order", order(key, qty)));
  }

  private Data status(String id, String user, String value) {
    return object("updateOrderStatus", user, of("orderId", id, "status", value));
  }

  private long count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Long.class, args);
  }

  private void platformUser(String openid) {
    PlatformUser user = new PlatformUser();
    user.setOpenid(openid);
    user.setRole(PlatformRole.SUPER.code());
    user.setStatus("ACTIVE");
    tx.execute(s -> platformUsers.insert(Entities.create(user)));
  }

  @BeforeEach
  void setup() {
    tx = new TransactionTemplate(manager);
    admin = "admin_" + id();
    seller = "seller_" + id();
    buyer = "buyer_" + id();
    when(wechat.send(any(Data.class))).thenReturn(of("errcode", 0));
    tx.execute(s -> {
      NotificationTemplate template = templates.selectByKey(TEMPLATE_KEY);
      template.setEnabled(false);
      template.setTemplateId("");
      return templates.updateConfig(Entities.update(template));
    });
    platformUser(admin);
    application = object("submitMerchantApplication", seller, of("application",
        of("storeName", "测试零食铺🍪", "ownerName", "经营者", "phone", "13800000000",
            "businessCategories", Arrays.asList("零食"))));
    call("markApplicationContacted", admin, of("applicationId", application.text("_id")));
    call("setPaymentPending", admin, of("applicationId", application.text("_id"), "feeAmount", 12300));
    call("confirmOfflinePayment", admin, of("applicationId", application.text("_id")));
    storeId = object("approveMerchantApplication", admin, of("applicationId", application.text("_id"))).text("storeId");
    productId = object("saveProduct", seller, of("product",
        of("storeId", storeId, "name", "饼干🍪", "price", 500, "stock", 10))).text("_id");
  }

  @Test
  void migrationJsonContractsAndAtomicApproval() {
    assertEquals(15, ((List<?>) call("listCollectionSchemas", admin, new Data())).size());
    assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class) >= 1);
    Data identity = object("getMyIdentity", seller, new Data());
    assertTrue(identity.array("roles").contains("MERCHANT_OWNER"));
    assertEquals("测试零食铺🍪", identity.object("store").text("name"));
    assertTrue(identity.object("store").flag("isOpen"));
    Data detail = object("getMerchantApplicationDetail", admin, of("applicationId", application.text("_id")));
    assertEquals(1, detail.array("payments").size());
    assertEquals("APPROVED", detail.object("application").text("status"));
    assertEquals("零食", detail.object("application").array("businessCategories").get(0));
    assertEquals(4, count("SELECT COUNT(*) FROM audit_logs WHERE targetId=?", application.text("_id")));
    assertThrows(BusinessException.class,
        () -> call("requestMoreInformation", admin, of("applicationId", application.text("_id"), "message", "补资料")));
    assertThrows(BusinessException.class,
        () -> call("markApplicationContactFailed", admin, of("applicationId", application.text("_id"))));
    assertNull(call("getMyStore", buyer, new Data()));
    assertEquals(true, call("initCollections", buyer, new Data()));
    assertNull(users.selectByOpenid("never-created", false));
    call("syncChineseDescriptions", admin, new Data());
  }

  @Test
  void duplicateCreateReturnsOriginalAndChangedRequestConflicts() throws Exception {
    List<Object> results = race(() -> create(buyer, "same-request", 2), () -> create(buyer, "same-request", 2));
    assertTrue(results.get(0) instanceof Data);
    assertTrue(results.get(1) instanceof Data);
    assertEquals(((Data) results.get(0)).text("_id"), ((Data) results.get(1)).text("_id"));
    assertFalse(((Data) results.get(0)).containsKey("requestHash"));
    assertEquals(1, orders.selectByOwner(buyer, null, 100).size());
    assertEquals(8, products.selectById(productId, false).getStock());
    BusinessException error = assertThrows(BusinessException.class,
        () -> call("createOrder", buyer, of("order", order("same-request", 3))));
    assertEquals("IDEMPOTENCY_CONFLICT", error.code);
  }

  @Test
  void competingBuyersNeverOversellAndFailuresRollback() throws Exception {
    List<Object> results = race(() -> create(buyer, "request-one", 8), () -> create("other_" + buyer, "request-two", 8));
    assertEquals(1, results.stream().filter(Data.class::isInstance).count());
    assertEquals(1, results.stream().filter(BusinessException.class::isInstance).count());
    assertEquals(2, products.selectById(productId, false).getStock());
    assertEquals(1, count("SELECT COUNT(*) FROM orders WHERE storeId=?", storeId));
    assertThrows(BusinessException.class,
        () -> call("createOrder", buyer, of("order", order("bad-phone", 1).with("phone", "invalid"))));
    assertEquals(2, products.selectById(productId, false).getStock());
  }

  @Test
  void duplicateCancelAndCompleteHaveOneInventoryEffect() throws Exception {
    String canceled = create(buyer, "cancel-request", 2).text("_id");
    List<Object> cancellations = race(() -> status(canceled, buyer, "CANCELED"),
        () -> status(canceled, seller, "REJECTED"));
    assertEquals(1, cancellations.stream().filter(Data.class::isInstance).count());
    assertEquals(10, products.selectById(productId, false).getStock());
    String completed = create(buyer, "finish-request", 3).text("_id");
    status(completed, seller, "ACCEPTED");
    status(completed, seller, "DELIVERING");
    status(completed, seller, "DELIVERED");
    List<Object> completions = race(() -> status(completed, buyer, "COMPLETED"),
        () -> status(completed, seller, "COMPLETED"));
    assertEquals(1, completions.stream().filter(Data.class::isInstance).count());
    assertEquals(3, products.selectById(productId, false).getSold());
    assertEquals(1, stores.selectById(storeId, false).getMonthlySales());
    assertEquals(5, orders.selectById(completed, false).getStatusHistory().size());
    Data stats = object("getMerchantStats", seller, new Data());
    assertEquals(1500, stats.number("todayAmount"));
  }

  @Test
  void productVersionPreventsLostStockAndDuplicateItemsMerge() {
    Data stale = object("getProduct", seller, of("productId", productId));
    Data input = order("merge-request", 1).with("items",
        Arrays.asList(of("productId", productId, "quantity", 1), of("productId", productId, "quantity", 2)));
    Data result = object("createOrder", buyer, of("order", input));
    assertEquals(1, result.array("items").size());
    assertEquals(1500, result.number("goodsAmount"));
    assertThrows(BusinessException.class,
        () -> call("saveProduct", seller, of("product", stale.with("name", "旧库存保存"))));
    assertEquals(7, products.selectById(productId, false).getStock());
    assertThrows(BusinessException.class, () -> object("getProduct", buyer, of("productId", productId)));
  }

  @Test
  void concurrentApprovalCreatesOnlyOneStoreAndOneAudit() throws Exception {
    String applicant = "new_" + id();
    String otherAdmin = "other_" + id();
    platformUser(otherAdmin);
    String id = object("submitMerchantApplication", applicant, of("application",
        of("storeName", "并发申请", "ownerName", "同学", "phone", "13800000000"))).text("_id");
    call("setPaymentPending", admin, of("applicationId", id));
    call("confirmOfflinePayment", admin, of("applicationId", id));
    List<Object> results = race(() -> call("approveMerchantApplication", admin, of("applicationId", id)),
        () -> call("approveMerchantApplication", otherAdmin, of("applicationId", id)));
    assertEquals(1, results.stream().filter(Data.class::isInstance).count());
    assertNotNull(stores.selectByOwner(applicant, false));
    assertEquals(1, count("SELECT COUNT(*) FROM audit_logs WHERE targetId=? AND action=?",
        id, "APPROVE_MERCHANT_APPLICATION"));
  }

  private void enableNotices(String stage) {
    Data config = object("getNotificationAdminConfig", admin, new Data())
        .with("enabled", true, "templateId", "test-template", "buyerOrderPushStage", stage);
    call("saveNotificationTemplate", admin, of("template", config));
  }

  private String grant(String user) {
    return object("recordSubscribePermission", user,
        of("permission", "ACCEPT", "scene", "ORDER_SUBMIT", "subscriptionRole", "BUYER")).text("grantId");
  }

  @Test
  void orderBoundGrantIsSingleUseAndWechatOnlyAtSelectedStage() throws Exception {
    enableNotices("DELIVERED");
    String grant = grant(buyer);
    String id = object("createOrder", buyer,
        of("order", order("grant-request", 1).with("buyerNotificationGrantId", grant))).text("_id");
    assertThrows(BusinessException.class, () -> call("createOrder", buyer,
        of("order", order("rebind-request", 1).with("buyerNotificationGrantId", grant))));
    assertEquals(9, products.selectById(productId, false).getStock());
    status(id, seller, "ACCEPTED");
    verifyNoInteractions(wechat);
    status(id, seller, "DELIVERING");
    status(id, seller, "DELIVERED");
    verify(wechat, times(1)).send(any(Data.class));
    assertEquals("CONSUMED", grants.selectById(grant, false).getStatus());
    String notificationId = jdbc.queryForObject("SELECT _id FROM notifications WHERE bizId=? AND type=?",
        String.class, id, "ORDER_DELIVERED");
    Notification n = notifications.selectById(notificationId, false);
    assertTrue(n.getPagePath().contains("backend=cloudrun"));
    race(() -> delivery.send(n.getId()), () -> delivery.send(n.getId()));
    verify(wechat, times(1)).send(any(Data.class));
    assertEquals("SUCCESS",
        deliveries.selectByNotification(n.getId(), "WECHAT_SUBSCRIBE", false).getDeliveryStatus());
    grant(buyer);
    String noGrant = create(buyer, "no-grant-request", 1).text("_id");
    status(noGrant, seller, "ACCEPTED");
    status(noGrant, seller, "DELIVERING");
    status(noGrant, seller, "DELIVERED");
    verify(wechat, times(1)).send(any(Data.class));
  }

  @Test
  void externalSendFailureDoesNotFailCommittedOrder() {
    enableNotices("ACCEPTED");
    String grant = grant(buyer);
    String id = object("createOrder", buyer,
        of("order", order("error-request", 1).with("buyerNotificationGrantId", grant))).text("_id");
    when(wechat.send(any(Data.class))).thenThrow(new IllegalStateException("timeout"));
    assertEquals("ACCEPTED", status(id, seller, "ACCEPTED").text("status"));
    assertEquals("UNKNOWN", grants.selectById(grant, false).getStatus());
    String notificationId = jdbc.queryForObject(
        "SELECT notificationId FROM notification_deliveries WHERE subscriptionGrantId=?", String.class, grant);
    NotificationDelivery sent = deliveries.selectByNotification(notificationId, "WECHAT_SUBSCRIBE", false);
    assertEquals("UNKNOWN", sent.getDeliveryStatus());
  }

  @Test
  void initializationKeepsAdminConfigAndFullUnreadCountsExceed100() {
    enableNotices("ACCEPTED");
    system.run(null);
    assertTrue(object("getNotificationConfig", buyer, new Data()).flag("enabled"));
    tx.execute(s -> {
      for (int i = 0; i < 105; i++) {
        notices.create(notices.notice(buyer, "CUSTOMER", "SYSTEM", "TEST_NOTIFICATION", "测试", "测试",
            "SYSTEM", "test", "/pages/messages/index", "count:" + i), false);
      }
      return null;
    });
    assertEquals(105, object("getUnreadNotificationCount", buyer, new Data()).number("total"));
    List<?> list = (List<?>) call("listNotifications", buyer, new Data());
    assertEquals(100, list.size());
    call("markAllNotificationsRead", buyer, new Data());
    assertEquals(0, object("getUnreadNotificationCount", buyer, new Data()).number("total"));
    verifyNoInteractions(wechat);
    tx.execute(s -> {
      for (String openid : Arrays.asList("Case_" + buyer, "case_" + buyer)) {
        User user = new User();
        user.setOpenid(openid);
        user.setRoles(Collections.singletonList("CUSTOMER"));
        user.setStatus("ACTIVE");
        users.insert(Entities.create(user));
      }
      return null;
    });
  }

  @SafeVarargs
  private final List<Object> race(Supplier<Object>... tasks) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(tasks.length);
    CountDownLatch ready = new CountDownLatch(tasks.length);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Object>> futures = new ArrayList<>();
      for (Supplier<Object> task : tasks) {
        futures.add(pool.submit(() -> {
          ready.countDown();
          start.await();
          try {
            return task.get();
          } catch (RuntimeException error) {
            return error;
          }
        }));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      List<Object> result = new ArrayList<>();
      for (Future<Object> future : futures) {
        result.add(future.get(30, TimeUnit.SECONDS));
      }
      return result;
    } finally {
      start.countDown();
      pool.shutdownNow();
    }
  }
}
