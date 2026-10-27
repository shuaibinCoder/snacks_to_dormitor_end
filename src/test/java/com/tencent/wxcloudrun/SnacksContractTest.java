package com.tencent.wxcloudrun;

import com.tencent.wxcloudrun.config.*;
import com.tencent.wxcloudrun.controller.SnacksController;
import com.tencent.wxcloudrun.dto.*;
import com.tencent.wxcloudrun.service.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.Action;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.tencent.wxcloudrun.dto.Data.*;

class SnacksContractTest {
  static final String[] ACTIONS = ("initCollections getMyIdentity submitMerchantApplication getMyMerchantApplication "
      + "listMerchantApplications getMerchantApplicationDetail markApplicationContacted markApplicationContactFailed "
      + "requestMoreInformation setPaymentPending confirmOfflinePayment approveMerchantApplication "
      + "rejectMerchantApplication listStores getStore getMyStore saveStore setStoreOpen listProducts getProduct "
      + "saveProduct toggleProduct createOrder listOrders getOrder updateOrderStatus getMerchantStats "
      + "getAdminDashboard listAdminStores setStorePlatformStatus listPlatformUsers savePlatformUser "
      + "getNotificationConfig getNotificationAdminConfig saveNotificationTemplate recordSubscribePermission "
      + "sendTestNotification getMyNotificationDiagnostics listNotifications getUnreadNotificationCount "
      + "markNotificationRead markAllNotificationsRead listSystemDictionaries listCollectionSchemas "
      + "syncChineseDescriptions").split(" ");
  private SnacksService service;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    service = mock(SnacksService.class);
    mvc = MockMvcBuilders.standaloneSetup(new SnacksController(service, new WechatIdentityResolver("wx-test")))
        .setControllerAdvice(new SnacksExceptionHandler())
        .build();
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String action) {
    return post("/api/snacks").contentType("application/json")
        .header("X-WX-SOURCE", "miniapp")
        .header("X-WX-APPID", "wx-test")
        .header("X-WX-OPENID", "gateway-user")
        .content("{\"action\":\"" + action + "\",\"openid\":\"forged-user\"}");
  }

  @Test
  void all45ActionsHaveExplicitMappings() {
    IdentityService identity = mock(IdentityService.class);
    ApplicationService applications = mock(ApplicationService.class);
    CatalogService catalog = mock(CatalogService.class);
    OrderService orders = mock(OrderService.class);
    PlatformService platform = mock(PlatformService.class);
    NotificationService notices = mock(NotificationService.class);
    SnacksService real = new SnacksService(identity, applications, catalog, orders, platform, notices);
    assertEquals(45, real.supportedActions().size());
    assertEquals(new HashSet<>(Arrays.asList(ACTIONS)), real.supportedActions());
    for (String action : ACTIONS) {
      assertDoesNotThrow(() -> real.execute(
          of("action", action, "storeId", "store", "orderId", "order", "notificationId", "notice"), "user"), action);
    }
    verify(applications).operate(eq(Action.APPROVE_MERCHANT_APPLICATION), any(Data.class), eq("user"));
    verify(orders).create(any(Data.class), eq("user"));
    verify(notices).saveConfig(any(Data.class), eq("user"));
    assertEquals("UNKNOWN_ACTION",
        assertThrows(BusinessException.class, () -> real.execute(of("action", "bad"), "user")).code);
  }

  @Test
  void all45ActionsUseCompatibleEnvelopeAndGatewayIdentity() throws Exception {
    for (String action : ACTIONS) {
      when(service.execute(any(Data.class), eq("gateway-user")))
          .thenReturn(of("_id", "cr_test", "createdAt", 1234567890123L, "items", Arrays.asList(of("price", 123))));
      mvc.perform(request(action))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data._id").value("cr_test"))
          .andExpect(jsonPath("$.data.items[0].price").value(123));
    }
    verify(service, times(45)).execute(any(Data.class), eq("gateway-user"));
  }

  @Test
  void nullBooleanArrayAndErrorsKeepProtocol() throws Exception {
    when(service.execute(any(Data.class), anyString())).thenReturn(null, true, Collections.emptyList());
    mvc.perform(request("getMyStore")).andExpect(content().json("{\"success\":true,\"data\":null}"));
    mvc.perform(request("initCollections")).andExpect(jsonPath("$.data").value(true));
    mvc.perform(request("listStores")).andExpect(jsonPath("$.data").isArray());
    when(service.execute(any(Data.class), anyString()))
        .thenThrow(new BusinessException("未知操作", "UNKNOWN_ACTION", 200));
    mvc.perform(request("bad")).andExpect(jsonPath("$.code").value("UNKNOWN_ACTION"));
    when(service.execute(any(Data.class), anyString()))
        .thenThrow(new IllegalStateException("private SQL and configuration"));
    mvc.perform(request("listStores"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("{\"success\":false,\"code\":\"INTERNAL_ERROR\","
            + "\"message\":\"服务暂时不可用，请稍后重试\"}"));
  }

  @Test
  void missingMismatchedAndReusedIdentityRejected() throws Exception {
    mvc.perform(post("/api/snacks").contentType("application/json")
        .content("{\"action\":\"getMyIdentity\",\"openid\":\"fake\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/snacks").contentType("application/json")
        .header("X-WX-SOURCE", "miniapp")
        .header("X-WX-APPID", "wrong")
        .header("X-WX-OPENID", "user")
        .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(request("getMyIdentity").header("X-WX-FROM-APPID", "reused-app"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void badJsonRejected() throws Exception {
    mvc.perform(request("getMyIdentity").content("{broken"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
  }
}
