package com.tencent.wxcloudrun.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;
import java.util.stream.Collectors;

/** 读取固定中文字典，并在实体转响应后附加不影响持久化的展示字段。 */
@Component
public class Labels {
  public final List<Data> dictionaries;
  public final List<Data> schemas;
  private final Map<String, Map<String, String>> labels = new HashMap<>();

  /** 启动时加载开发期生成的字典与 15 张表字段说明。 */
  public Labels(ObjectMapper json) throws IOException {
    dictionaries = load(json, DICTIONARIES_RESOURCE);
    schemas = load(json, SCHEMAS_RESOURCE);
    for (Data d : dictionaries) {
      Map<String, String> items = new HashMap<>();
      for (Object item : d.array("items")) {
        Data value = new Data((Map<String, ?>) item);
        items.put(value.text("code"), value.text("label"));
      }
      labels.put(d.text("type"), items);
    }
  }

  private List<Data> load(ObjectMapper json, String name) throws IOException {
    try (InputStream in = new ClassPathResource("snacks/" + name + ".json").getInputStream()) {
      return json.readValue(in, new TypeReference<List<Data>>() {});
    }
  }

  /** 未配置的编码原样返回，避免新增状态导致接口不可用。 */
  public String label(String type, String value) {
    return labels.getOrDefault(type, Collections.emptyMap()).getOrDefault(value, value);
  }

  private void add(Data d, String field, String type) {
    if (d.containsKey(field)) {
      d.put(field + "Label", label(type, d.text(field)));
    }
  }

  /** 按实体类型补充中文标签；表名和字段名不由客户端指定。 */
  public Data decorate(Class<?> type, Data d) {
    if (d == null) {
      return null;
    }
    if (type == User.class) {
      d.put("roleLabels", d.array("roles").stream()
          .map(v -> label(DictionaryType.USER_ROLE.code(), String.valueOf(v)))
          .collect(Collectors.toList()));
      add(d, "status", DictionaryType.ACCOUNT_STATUS.code());
    } else if (type == PlatformUser.class) {
      add(d, "role", DictionaryType.PLATFORM_ROLE.code());
      add(d, "status", DictionaryType.ACCOUNT_STATUS.code());
    } else if (type == MerchantApplication.class) {
      add(d, "status", DictionaryType.APPLICATION_STATUS.code());
    } else if (type == MerchantPayment.class) {
      add(d, "status", DictionaryType.PAYMENT_STATUS.code());
      add(d, "paymentMethod", DictionaryType.PAYMENT_METHOD.code());
    } else if (type == Store.class) {
      add(d, "approvalStatus", DictionaryType.STORE_APPROVAL_STATUS.code());
      add(d, "platformStatus", DictionaryType.STORE_PLATFORM_STATUS.code());
      add(d, "businessStatus", DictionaryType.STORE_BUSINESS_STATUS.code());
      d.with("auditStatus", d.text("approvalStatus"), "isOpen",
                StoreBusinessStatus.OPEN.matches(d.text("businessStatus")));
    } else if (type == Product.class) {
      add(d, "status", DictionaryType.PRODUCT_STATUS.code());
    } else if (type == Order.class) {
      add(d, "status", DictionaryType.ORDER_STATUS.code());
    } else if (type == AuditLog.class) {
      add(d, "operatorRole", DictionaryType.PLATFORM_ROLE.code());
      add(d, "action", DictionaryType.AUDIT_ACTION.code());
      add(d, "targetType", DictionaryType.AUDIT_TARGET_TYPE.code());
    } else if (type == Notification.class) {
      add(d, "category", DictionaryType.NOTIFICATION_CATEGORY.code());
      add(d, "type", DictionaryType.NOTIFICATION_TYPE.code());
      add(d, "priority", DictionaryType.NOTIFICATION_PRIORITY.code());
      add(d, "bizType", DictionaryType.NOTIFICATION_BIZ_TYPE.code());
      add(d, "readStatus", DictionaryType.NOTIFICATION_READ_STATUS.code());
      DictionaryType roleType = PlatformRole.isPlatformRole(d.text("recipientRole"))
          ? DictionaryType.PLATFORM_ROLE : DictionaryType.USER_ROLE;
      add(d, "recipientRole", roleType.code());
    } else if (type == NotificationDelivery.class) {
      add(d, "channel", DictionaryType.NOTIFICATION_CHANNEL.code());
      add(d, "deliveryStatus", DictionaryType.NOTIFICATION_DELIVERY_STATUS.code());
    } else if (type == NotificationPreference.class) {
      add(d, "lastPermission", DictionaryType.SUBSCRIBE_PERMISSION.code());
      d.put("templateKeyLabel", "统一业务状态通知模板");
    } else if (type == NotificationTemplate.class) {
      add(d, "buyerOrderPushStage", DictionaryType.BUYER_ORDER_PUSH_STAGE.code());
    } else if (type == NotificationGrant.class) {
      d.put("statusLabel", label(DictionaryType.GRANT_STATUS.code(), d.text("status")));
    }
    add(d, "subscriptionRole", DictionaryType.NOTIFICATION_SUBSCRIPTION_ROLE.code());
    return d;
  }
}
