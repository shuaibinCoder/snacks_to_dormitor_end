package com.tencent.wxcloudrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import com.tencent.wxcloudrun.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BusinessRulesTest {
  /** 缺省或显式关闭初始化时，启动入口均不得调用数据写入 Mapper。 */
  @Test
  void startupInitializationIsDisabledByDefaultAndBySwitch() {
    for (String[] properties : Arrays.asList(new String[0],
        new String[]{"snacks.initialize-on-startup=false"})) {
      initializationContext().withPropertyValues(properties).run(context -> {
        assertNull(context.getStartupFailure());
        context.getBean(SystemDataService.class).run(null);
        verifyNoInteractions(context.getBean(SystemDictionaryMapper.class),
            context.getBean(SystemCollectionSchemaMapper.class),
            context.getBean(NotificationTemplateMapper.class));
      });
    }
  }

  /** 显式开启仅补缺省数据，不覆盖已有字典及管理员通知配置。 */
  @Test
  void startupInitializationRequiresExplicitOptIn() {
    initializationContext().withPropertyValues("snacks.initialize-on-startup=true").run(context -> {
      assertNull(context.getStartupFailure());
      context.getBean(SystemDataService.class).run(null);
      Labels labels = context.getBean(Labels.class);
      SystemDictionaryMapper dictionaries = context.getBean(SystemDictionaryMapper.class);
      SystemCollectionSchemaMapper schemas = context.getBean(SystemCollectionSchemaMapper.class);
      NotificationTemplateMapper templates = context.getBean(NotificationTemplateMapper.class);
      verify(dictionaries, times(labels.dictionaries.size())).insertIgnore(any());
      verify(schemas, times(labels.schemas.size())).insertIgnore(any());
      verify(templates).insertIgnore(any());
      verifyNoMoreInteractions(dictionaries, schemas, templates);
    });
  }

  /** 关闭启动初始化不影响管理员主动同步字典和字段说明。 */
  @Test
  void manualDictionaryRefreshRemainsAvailableWithStartupDisabled() {
    initializationContext().run(context -> {
      assertNull(context.getStartupFailure());
      context.getBean(SystemDataService.class).seed(true);
      Labels labels = context.getBean(Labels.class);
      verify(context.getBean(SystemDictionaryMapper.class), times(labels.dictionaries.size())).update(any());
      verify(context.getBean(SystemCollectionSchemaMapper.class), times(labels.schemas.size())).update(any());
      verifyNoInteractions(context.getBean(NotificationTemplateMapper.class));
    });
  }

  private ApplicationContextRunner initializationContext() {
    return new ApplicationContextRunner()
        .withBean(SystemDictionaryMapper.class, () -> mock(SystemDictionaryMapper.class))
        .withBean(SystemCollectionSchemaMapper.class, () -> mock(SystemCollectionSchemaMapper.class))
        .withBean(NotificationTemplateMapper.class, () -> mock(NotificationTemplateMapper.class))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(Labels.class)
        .withUserConfiguration(SystemDataService.class);
  }

  @Test
  void labelsAndInternalIdempotencyHashArePreserved() throws Exception {
    Labels labels = new Labels(new ObjectMapper());
    Data order = labels.decorate(Order.class, of("status", "CREATED", "requestHash", "hash"));
    assertEquals("待商家接单", order.text("statusLabel"));
    assertEquals("hash", order.text("requestHash"));
    Data store = labels.decorate(Store.class, of("approvalStatus", "APPROVED", "businessStatus", "OPEN"));
    assertTrue(store.flag("isOpen"));
    assertEquals("APPROVED", store.text("auditStatus"));
    assertEquals(15, labels.schemas.size());
    assertEquals("发送中",
        labels.decorate(NotificationDelivery.class, of("deliveryStatus", "SENDING")).text("deliveryStatusLabel"));
    assertEquals("结果未知",
        labels.decorate(NotificationDelivery.class, of("deliveryStatus", "UNKNOWN")).text("deliveryStatusLabel"));
    for (String status : Arrays.asList("AVAILABLE", "SENDING", "CONSUMED", "FAILED", "UNKNOWN")) {
      assertNotEquals(status, labels.decorate(NotificationGrant.class, of("status", status)).text("statusLabel"));
    }
    NotificationService notices = new NotificationService(null, null, null, null, null, null, null, labels, null, null);
    assertEquals(labels.label("APPLICATION_STATUS", "CONTACTED"),
        notices.notice("user", "CUSTOMER", "MERCHANT_APPLICATION", "APPLICATION_CONTACTED", "已联系",
            "CONTACTED", "MERCHANT_APPLICATION", "id", "/pages/messages/index", "key").text("statusText"));
  }

  /** 缺记录的管理表单、公开配置及诊断均可读，读取不补数据或开启发送。 */
  @Test
  void missingNotificationTemplateReturnsDisabledDraftWithoutWriting() throws Exception {
    NotificationTemplateMapper templates = mock(NotificationTemplateMapper.class);
    NotificationService service = notificationService(templates, mock(IdentityService.class));
    Data admin = service.adminConfig();
    assertEquals(TEMPLATE_KEY, admin.text("key"));
    assertEquals("", admin.text("templateId"));
    assertFalse(admin.flag("enabled"));
    assertEquals("trial", admin.text("miniprogramState"));
    assertEquals("DELIVERED", admin.text("buyerOrderPushStage"));
    assertEquals("phrase8", admin.object("fields").text("titleKey"));
    assertEquals(0, admin.number("createdAt"));
    assertFalse(service.publicConfig().flag("enabled"));
    assertFalse(service.diagnostics("buyer").object("config").flag("enabled"));
    verify(templates, times(3)).selectByKey(TEMPLATE_KEY);
    verifyNoMoreInteractions(templates);
    NotificationTemplate draft = service.config();
    draft.getFields().put("titleKey", "changed");
    assertEquals("phrase8", service.config().getFields().text("titleKey"));
  }

  /** 首次主动保存补固定模板，之后保存保留主键、创建时间及内部策略。 */
  @Test
  void notificationTemplateCanBeCreatedAndUpdatedFromAdminSave() throws Exception {
    NotificationTemplateMapper templates = mock(NotificationTemplateMapper.class);
    IdentityService identity = mock(IdentityService.class);
    PlatformUser operator = new PlatformUser();
    when(identity.requirePlatform("admin", codes(PlatformRole.ADMIN, PlatformRole.SUPER))).thenReturn(operator);
    java.util.concurrent.atomic.AtomicReference<NotificationTemplate> stored = new java.util.concurrent.atomic.AtomicReference<>();
    when(templates.insertIgnore(any())).thenAnswer(invocation -> {
      if (stored.get() != null) {
        return 0;
      }
      stored.set(invocation.getArgument(0));
      return 1;
    });
    when(templates.selectByKeyForUpdate(TEMPLATE_KEY)).thenAnswer(invocation -> stored.get());
    when(templates.updateConfig(any())).thenReturn(1);
    NotificationService service = notificationService(templates, identity);
    Data first = service.saveConfig(notificationInput().with("key", "untrusted", "_id", "untrusted",
        "policyVersion", "untrusted", "createdAt", 1), "admin");
    assertEquals(TEMPLATE_KEY, first.text("_id"));
    assertEquals(TEMPLATE_KEY, first.text("key"));
    assertEquals(NOTIFICATION_POLICY_VERSION, first.text("policyVersion"));
    assertEquals("wx-template", first.text("templateId"));
    assertTrue(first.flag("enabled"));
    assertTrue(first.number("createdAt") > 1);
    long createdAt = first.number("createdAt");
    stored.get().setId("legacy-template-id");
    Data second = service.saveConfig(notificationInput().with("templateId", "second-template", "enabled", false), "admin");
    assertEquals("legacy-template-id", second.text("_id"));
    assertEquals(createdAt, second.number("createdAt"));
    assertEquals("second-template", second.text("templateId"));
    assertFalse(second.flag("enabled"));
    verify(identity).audit(eq(operator), eq(AuditAction.SAVE_NOTIFICATION_TEMPLATE.code()),
        eq(AuditTargetType.NOTIFICATION_TEMPLATE.code()), eq(TEMPLATE_KEY),
        argThat((Data before) -> !before.flag("enabled") && before.text("templateId").isEmpty()),
        argThat((Data after) -> after.flag("enabled") && "wx-template".equals(after.text("templateId"))), anyString());
    verify(identity).audit(eq(operator), eq(AuditAction.SAVE_NOTIFICATION_TEMPLATE.code()),
        eq(AuditTargetType.NOTIFICATION_TEMPLATE.code()), eq(TEMPLATE_KEY),
        argThat((Data before) -> before.flag("enabled") && "wx-template".equals(before.text("templateId"))),
        argThat((Data after) -> !after.flag("enabled") && "second-template".equals(after.text("templateId"))), anyString());
    org.mockito.InOrder order = inOrder(templates);
    for (int i = 0; i < 2; i++) {
      order.verify(templates).insertIgnore(any());
      order.verify(templates).selectByKeyForUpdate(TEMPLATE_KEY);
      order.verify(templates).updateConfig(any());
    }
  }

  /** 无权限或参数非法时不能通过首次保存补模板。 */
  @Test
  void invalidOrUnauthorizedTemplateSaveDoesNotInitializeRecord() throws Exception {
    NotificationTemplateMapper templates = mock(NotificationTemplateMapper.class);
    IdentityService identity = mock(IdentityService.class);
    NotificationService service = notificationService(templates, identity);
    when(identity.requirePlatform("buyer", codes(PlatformRole.ADMIN, PlatformRole.SUPER)))
        .thenThrow(new BusinessException("没有配置权限"));
    assertThrows(BusinessException.class, () -> service.saveConfig(notificationInput(), "buyer"));
    for (Data invalid : Arrays.asList(
        notificationInput().with("buyerOrderPushStage", "CREATED"),
        notificationInput().with("miniprogramState", "invalid"),
        notificationInput().with("templateId", ""),
        notificationInput().with("fields", of("titleKey", "phrase8", "statusKey", "phrase8", "timeKey", "time3", "remarkKey", "thing2")),
        notificationInput().with("fields", new Data()))) {
      assertThrows(BusinessException.class, () -> service.saveConfig(invalid, "admin"));
    }
    verifyNoInteractions(templates);
    verify(identity, never()).audit(any(), anyString(), anyString(), anyString(), any(), any(), anyString());
  }

  /** 缺表及数据库故障不能伪装成未配置草稿；写入和审计错误必须继续向事务入口抛出。 */
  @Test
  void notificationDatabaseAndAuditFailuresAreNotSwallowed() throws Exception {
    NotificationTemplateMapper templates = mock(NotificationTemplateMapper.class);
    IdentityService identity = mock(IdentityService.class);
    NotificationService service = notificationService(templates, identity);
    RuntimeException failure = new org.springframework.dao.DataAccessResourceFailureException("数据库不可用");
    when(templates.selectByKey(TEMPLATE_KEY)).thenThrow(failure);
    assertSame(failure, assertThrows(RuntimeException.class, service::adminConfig));
    assertSame(failure, assertThrows(RuntimeException.class, service::publicConfig));
    when(templates.insertIgnore(any())).thenThrow(failure);
    assertSame(failure, assertThrows(RuntimeException.class, () -> service.saveConfig(notificationInput(), "admin")));
    verify(templates, never()).updateConfig(any());
    reset(templates);
    when(templates.selectByKeyForUpdate(TEMPLATE_KEY)).thenReturn(Entities.create(NotificationTemplate.defaultConfig()));
    when(templates.updateConfig(any())).thenReturn(1);
    doThrow(failure).when(identity).audit(any(), anyString(), anyString(), anyString(), any(), any(), anyString());
    assertSame(failure, assertThrows(RuntimeException.class, () -> service.saveConfig(notificationInput(), "admin")));
  }

  private NotificationService notificationService(NotificationTemplateMapper templates, IdentityService identity) throws Exception {
    ObjectMapper json = new ObjectMapper();
    Labels labels = new Labels(json);
    return new NotificationService(templates, mock(NotificationPreferenceMapper.class), null, null,
        mock(NotificationDeliveryMapper.class), null, null, labels, identity, new EntityView(json, labels));
  }

  private Data notificationInput() {
    return of("templateId", "wx-template", "enabled", true, "miniprogramState", "trial",
        "buyerOrderPushStage", "DELIVERED",
        "fields", of("titleKey", "phrase8", "statusKey", "phrase23", "timeKey", "time3", "remarkKey", "thing2"));
  }

  @Test
  void amountsQuantitiesAndContactFieldsAreValidated() {
    for (Object value : Arrays.asList(-1, "NaN", "Infinity", 1.1, "1e100")) {
      assertThrows(BusinessException.class, () -> of("amount", value).integer("amount", 0, 0, 100000000));
    }
    assertEquals(123, of("amount", "123").integer("amount", 0, 0, 999));
    assertThrows(BusinessException.class,
        () -> CatalogService.storeFields(of("name", "小店", "ownerName", "同学", "phone", "123"), false));
    assertThrows(BusinessException.class, () -> of("items", "not-array").array("items"));
    assertThrows(BusinessException.class, () -> of("product", "not-object").object("product"));
  }

  @Test
  void orderStateMatrixRejectsRepeatedAndUnauthorizedEffects() {
    assertTrue(OrderService.allowed("CREATED", "ACCEPTED", true, false));
    assertTrue(OrderService.allowed("CREATED", "CANCELED", false, true));
    assertTrue(OrderService.allowed("DELIVERED", "COMPLETED", false, true));
    assertFalse(OrderService.allowed("COMPLETED", "COMPLETED", true, true));
    assertFalse(OrderService.allowed("CANCELED", "CANCELED", false, true));
    assertFalse(OrderService.allowed("CREATED", "ACCEPTED", false, true));
    assertFalse(OrderService.allowed("CREATED", "CANCELED", false, false));
    assertFalse(OrderService.allowed("CREATED", "DELIVERED", true, false));
  }

  @Test
  void ordinaryAdminCannotDemotePeerOrSuperAdmin() {
    PlatformUserMapper users = mock(PlatformUserMapper.class);
    IdentityService identity = mock(IdentityService.class);
    PlatformUser operator = new PlatformUser();
    operator.setOpenid("admin");
    operator.setRole(PlatformRole.ADMIN.code());
    when(identity.requirePlatform("admin", codes(PlatformRole.ADMIN, PlatformRole.SUPER))).thenReturn(operator);
    PlatformService platform = platform(users, identity);
    for (String role : Arrays.asList(codes(PlatformRole.ADMIN, PlatformRole.SUPER))) {
      PlatformUser old = new PlatformUser();
      old.setId("peer-id");
      old.setOpenid("peer");
      old.setRole(role);
      when(users.selectByOpenid("peer", null, true)).thenReturn(old);
      assertThrows(BusinessException.class, () -> platform.saveUser(
          of("platformUser", of("openid", "peer", "role", PlatformRole.OPERATOR.code(), "status", AccountStatus.DISABLED.code())), "admin"));
    }
    verify(users, never()).update(any());
    verify(users, never()).insert(any());
  }

  @Test
  void applicationResubmitValidatesLockedLatestState() {
    MerchantApplicationMapper applications = mock(MerchantApplicationMapper.class);
    IdentityService identity = mock(IdentityService.class);
    MerchantApplication old = new MerchantApplication();
    old.setId("application");
    old.setStatus("APPROVED");
    when(applications.selectLatest("applicant", true)).thenReturn(old);
    ApplicationService service = new ApplicationService(applications, null, null, null, identity, null, null);
    assertThrows(BusinessException.class, () -> service.submit(
        of("application", of("storeName", "测试店铺", "ownerName", "经营者", "phone", "13800000000")), "applicant"));
    verify(applications, never()).updateSubmission(any());
  }

  @Test
  void platformContactAndIdentityMustBeValid() {
    PlatformUserMapper users = mock(PlatformUserMapper.class);
    IdentityService identity = mock(IdentityService.class);
    PlatformUser operator = new PlatformUser();
    operator.setOpenid("admin");
    operator.setRole(PlatformRole.SUPER.code());
    when(identity.requirePlatform("admin", codes(PlatformRole.ADMIN, PlatformRole.SUPER))).thenReturn(operator);
    PlatformService service = platform(users, identity);
    assertThrows(BusinessException.class, () -> service.saveUser(
        of("platformUser", of("openid", "target", "role", PlatformRole.OPERATOR.code(), "phone", "123")), "admin"));
    assertThrows(BusinessException.class, () -> service.saveUser(
        of("platformUser", of("openid", "invalid id", "role", PlatformRole.OPERATOR.code())), "admin"));
    verifyNoInteractions(users);
  }

  private PlatformService platform(PlatformUserMapper users, IdentityService identity) {
    return new PlatformService(null, null, null, users, null, null, identity, null, null, null);
  }

  @Test
  void resourceOwnershipAndDisabledUserAreEnforced() {
    UserMapper users = mock(UserMapper.class);
    StoreMapper stores = mock(StoreMapper.class);
    IdentityService identity = new IdentityService(users, null, stores, null, null, null);
    Store store = new Store();
    store.setOwnerOpenid("other");
    store.setApprovalStatus("APPROVED");
    store.setPlatformStatus("ACTIVE");
    when(stores.selectById("store", true)).thenReturn(store);
    assertThrows(BusinessException.class, () -> identity.owned("user", "store", true));
    User disabled = new User();
    disabled.setStatus("DISABLED");
    when(users.selectByOpenid("user", true)).thenReturn(disabled);
    assertEquals(403, assertThrows(BusinessException.class, () -> identity.ensure("user")).httpStatus);
  }

  @Test
  void staleInventoryEditDoesNotWrite() {
    ProductMapper products = mock(ProductMapper.class);
    IdentityService identity = mock(IdentityService.class);
    Product old = new Product();
    old.setId("product");
    old.setStoreId("store");
    old.setVersion(2);
    when(products.selectById("product", true)).thenReturn(old);
    CatalogService catalog = new CatalogService(null, products, null, identity, null);
    assertThrows(BusinessException.class, () -> catalog.saveProduct(of("product",
        of("_id", "product", "storeId", "store", "version", 1, "name", "饼干", "price", 500, "stock", 9)), "owner"));
    verify(products, never()).updateDetails(any(), anyLong());
  }

  @Test
  void grantBindingMustSatisfyAtomicSqlConditions() {
    NotificationGrantMapper grants = mock(NotificationGrantMapper.class);
    NotificationService notices = new NotificationService(null, null, grants, null, null, null, null, null, null, null);
    assertThrows(BusinessException.class, () -> notices.bind("buyer", "grant", "order"));
    verify(grants).bindOrder(eq("grant"), eq("buyer"), eq(TEMPLATE_KEY), eq("order"), anyLong());
  }

  @Test
  void wechatUsesHttpFieldsAndDoesNotFabricateSuccess() {
    Data notification = of("recipientOpenid", "user", "type", "ORDER_DELIVERED", "statusText", "已送达",
        "summary", "商家已送达", "createdAt", 0, "pagePath", "/pages/order/index?id=cr_1&backend=cloudrun");
    Data config = of("templateId", "template", "miniprogramState", "trial",
        "fields", of("titleKey", "phrase8", "statusKey", "phrase23", "timeKey", "time3", "remarkKey", "thing2"));
    Data payload = NotificationDeliveryService.payload(notification, config);
    assertEquals("template", payload.text("template_id"));
    assertEquals("trial", payload.text("miniprogram_state"));
    assertEquals("1970-01-01 08:00", payload.object("data").object("time3").text("value"));
    RestTemplate http = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.createServer(http);
    RestTemplateBuilder builder = mock(RestTemplateBuilder.class, RETURNS_SELF);
    when(builder.build()).thenReturn(http);
    WechatMessageClient client = new WechatMessageClient(builder);
    server.expect(requestTo("http://api.weixin.qq.com/cgi-bin/message/subscribe/send"))
        .andRespond(withSuccess("{\"errcode\":43101,\"errmsg\":\"refused\"}", MediaType.APPLICATION_JSON));
    assertEquals("43101", client.send(payload).text("errcode"));
    server.verify();
    server.reset();
    server.expect(requestTo("http://api.weixin.qq.com/cgi-bin/message/subscribe/send"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    assertThrows(IllegalStateException.class, () -> client.send(payload));
    server.verify();
  }
}
