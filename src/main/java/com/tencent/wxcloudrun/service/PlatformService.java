package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 平台工作台、店铺治理、平台人员授权和系统字典查询。 */
@Service
public class PlatformService {
  private final MerchantApplicationMapper applications;
  private final StoreMapper stores;
  private final AuditLogMapper auditLogs;
  private final PlatformUserMapper platformUsers;
  private final SystemDictionaryMapper dictionaries;
  private final SystemCollectionSchemaMapper schemas;
  private final IdentityService identity;
  private final NotificationService notices;
  private final SystemDataService system;
  private final EntityView view;

  public PlatformService(MerchantApplicationMapper applications, StoreMapper stores, AuditLogMapper auditLogs,
      PlatformUserMapper platformUsers, SystemDictionaryMapper dictionaries, SystemCollectionSchemaMapper schemas,
      IdentityService identity, NotificationService notices, SystemDataService system, EntityView view) {
    this.applications = applications;
    this.stores = stores;
    this.auditLogs = auditLogs;
    this.platformUsers = platformUsers;
    this.dictionaries = dictionaries;
    this.schemas = schemas;
    this.identity = identity;
    this.notices = notices;
    this.system = system;
    this.view = view;
  }

  /** 聚合平台待办、店铺数量和最近审计，仅平台人员可访问。 */
  public Data dashboard(String openid) {
    PlatformUser operator = identity.requirePlatform(openid,
        codes(PlatformRole.OPERATOR, PlatformRole.ADMIN, PlatformRole.SUPER));
    long pending = 0;
    for (String status : Arrays.asList(codes(ApplicationStatus.SUBMITTED, ApplicationStatus.CONTACT_FAILED,
        ApplicationStatus.CONTACTED, ApplicationStatus.PAYMENT_PENDING, ApplicationStatus.PAID))) {
      pending += applications.countByStatus(status);
    }
    return of("role", operator.getRole(), "pendingApplications", pending,
        "paymentPending", applications.countByStatus(ApplicationStatus.PAYMENT_PENDING.code()),
        "approvalPending", applications.countByStatus(ApplicationStatus.PAID.code()), "activeStores", stores.countActive(),
        "recentLogs", view.list(auditLogs.selectRecent(RECENT_AUDIT_LIMIT)));
  }

  /** 返回平台店铺治理列表，包含非公开经营状态。 */
  public List<Data> stores(String openid) {
    identity.requirePlatform(openid,
        codes(PlatformRole.OPERATOR, PlatformRole.ADMIN, PlatformRole.SUPER));
    return view.list(stores.selectAll(MAX_LIST_SIZE));
  }

  /** 平台暂停与商家打烊分别存储，恢复平台状态不会擅自开店。 */
  public boolean storeStatus(Data e, String openid) {
    PlatformUser operator = identity.requirePlatform(openid, codes(PlatformRole.ADMIN, PlatformRole.SUPER));
    Store store = stores.selectById(e.required("storeId", 64), true);
    require(store != null, "记录不存在");
    String status = e.text("platformStatus");
    require(oneOf(status, codes(StorePlatformStatus.ACTIVE, StorePlatformStatus.SUSPENDED)), "平台状态不正确");
    Data before = Entities.snapshot(store);
    Data patch = of("platformStatus", status);
    stores.updatePlatformStatus(store.getId(), status, now());
    boolean active = StorePlatformStatus.ACTIVE.matches(status);
    String type = active ? NotificationType.STORE_RESUMED.code() : NotificationType.STORE_SUSPENDED.code();
    identity.audit(operator, active ? AuditAction.RESUME_STORE.code() : AuditAction.SUSPEND_STORE.code(),
        AuditTargetType.STORE.code(), store.getId(), before, patch, e.text("reason"));
    notices.create(notices.notice(store.getOwnerOpenid(), UserRole.MERCHANT_OWNER.code(), NotificationCategory.STORE.code(), type,
        active ? "店铺已恢复展示和接单。" : "店铺已暂停展示和接单，请联系平台处理。", active ? "已恢复" : "已暂停",
        BizType.STORE.code(), store.getId(), MERCHANT_PAGE,
        type + ":" + store.getId() + ":" + now()), false);
    return true;
  }

  /** 返回平台人员列表，仅管理员和超级管理员可访问。 */
  public List<Data> team(String openid) {
    identity.requirePlatform(openid, codes(PlatformRole.ADMIN, PlatformRole.SUPER));
    return view.list(platformUsers.selectAll(MAX_LIST_SIZE));
  }

  /** 普通管理员只能维护运营人员，超级管理员才能维护同级管理员。 */
  public boolean saveUser(Data e, String openid) {
    PlatformUser operator = identity.requirePlatform(openid, codes(PlatformRole.ADMIN, PlatformRole.SUPER));
    Data input = e.object("platformUser");
    String target = input.required("openid", 64);
    String role = input.text("role");
    require(target.matches(OPENID_PATTERN), "用户身份格式不正确");
    require(input.text("phone").isEmpty() || input.text("phone").matches(PHONE_PATTERN), "请输入正确手机号");
    require(oneOf(role, codes(PlatformRole.OPERATOR, PlatformRole.ADMIN)), "角色不正确");
    require(PlatformRole.SUPER.matches(operator.getRole()) || PlatformRole.OPERATOR.matches(role),
        "只有超级管理员可以添加平台管理员");
    PlatformUser old = platformUsers.selectByOpenid(target, null, true);
    require(old == null || !PlatformRole.SUPER.matches(old.getRole()), "不能修改超级管理员");
    require(old == null || PlatformRole.SUPER.matches(operator.getRole())
        || PlatformRole.OPERATOR.matches(old.getRole()),
        "普通管理员不能修改同级管理员");
    String status = input.text("status", AccountStatus.ACTIVE.code());
    require(oneOf(status, codes(AccountStatus.ACTIVE, AccountStatus.DISABLED)), "账号状态不正确");
    Data before = Entities.snapshot(old);
    PlatformUser value = old == null ? new PlatformUser() : old;
    value.setOpenid(target);
    value.setRole(role);
    value.setDisplayName(input.text("displayName", "平台人员"));
    value.setPhone(input.text("phone"));
    value.setStatus(status);
    if (old == null) {
      value.setCreatedBy(openid);
      platformUsers.insert(Entities.create(value));
    } else {
      platformUsers.update(Entities.update(value));
    }
    identity.audit(operator, AuditAction.SAVE_PLATFORM_USER.code(), AuditTargetType.PLATFORM_USER.code(), value.getId(),
        before, Entities.snapshot(value), "");
    return true;
  }

  /** 查询字典或字段说明；主动同步仅允许管理员并写入审计。 */
  public Object dictionaries(Action action, String openid) {
    if (Action.SYNC_CHINESE_DESCRIPTIONS == action) {
      PlatformUser operator = identity.requirePlatform(openid, codes(PlatformRole.ADMIN, PlatformRole.SUPER));
      system.seed(true);
      identity.audit(operator, AuditAction.SYNC_CHINESE_DESCRIPTIONS.code(),
          AuditTargetType.SYSTEM_DICTIONARY.code(), SYSTEM_DATA_VERSION, null,
          of("version", SYSTEM_DATA_VERSION), "同步字典及字段说明；中文标签读取时生成");
      return of("version", SYSTEM_DATA_VERSION);
    }
    identity.requirePlatform(openid,
        codes(PlatformRole.OPERATOR, PlatformRole.ADMIN, PlatformRole.SUPER));
    return view.list(Action.LIST_COLLECTION_SCHEMAS == action
        ? schemas.selectAll() : dictionaries.selectAll());
  }
}
