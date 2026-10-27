package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.tencent.wxcloudrun.dto.Data.*;

/** 可信 OpenID 对应的应用身份、店铺归属、平台权限与审计写入。 */
@Service
public class IdentityService {
  private final UserMapper users;
  private final PlatformUserMapper platformUsers;
  private final StoreMapper stores;
  private final MerchantApplicationMapper applications;
  private final AuditLogMapper auditLogs;
  private final EntityView view;

  public IdentityService(UserMapper users, PlatformUserMapper platformUsers, StoreMapper stores,
      MerchantApplicationMapper applications, AuditLogMapper auditLogs, EntityView view) {
    this.users = users;
    this.platformUsers = platformUsers;
    this.stores = stores;
    this.applications = applications;
    this.auditLogs = auditLogs;
    this.view = view;
  }

  /** 首次访问只补默认顾客角色；已有角色和停用状态绝不重置。 */
  public User ensure(String openid) {
    require(!openid.isEmpty(), "缺少用户身份");
    User created = new User();
    created.setOpenid(openid);
    created.setRoles(Collections.singletonList(UserRole.CUSTOMER.code()));
    created.setStatus(AccountStatus.ACTIVE.code());
    users.insertIgnore(Entities.create(created));
    User user = users.selectByOpenid(openid, true);
    require(user != null, "用户身份初始化失败");
    if (!AccountStatus.ACTIVE.matches(user.getStatus())) {
      throw new BusinessException("账号已停用", ErrorCode.FORBIDDEN.code(), HTTP_STATUS_FORBIDDEN);
    }
    return user;
  }

  /** 查询仍有效的平台人员，不依据客户端传入角色授权。 */
  public PlatformUser platform(String openid) {
    return platformUsers.selectByOpenid(openid, AccountStatus.ACTIVE.code(), false);
  }

  /** 校验当前操作者属于允许的平台角色。 */
  public PlatformUser requirePlatform(String openid, String... allowed) {
    PlatformUser role = platform(openid);
    if (role == null || !oneOf(role.getRole(), allowed)) {
      throw new BusinessException("无平台管理权限", ErrorCode.FORBIDDEN.code(), HTTP_STATUS_FORBIDDEN);
    }
    return role;
  }

  /** 查询当前商家唯一店铺；没有店铺时返回 null。 */
  public Store owned(String openid) {
    return stores.selectByOwner(openid, false);
  }

  /** 我的店铺响应经过统一实体装饰，保留中文标签和 isOpen。 */
  public Data storeView(Store store) {
    return view.data(store);
  }

  /** 校验店铺归属、审核和平台状态；需要修改时按固定顺序加行锁。 */
  public Store owned(String openid, String id, boolean lock) {
    Store store = stores.selectById(id, lock);
    require(store != null, "记录不存在");
    require(openid.equals(store.getOwnerOpenid()), "无权操作该店铺");
    require(StoreApprovalStatus.APPROVED.matches(store.getApprovalStatus()), "店铺尚未通过平台审核");
    require(StorePlatformStatus.ACTIVE.matches(store.getPlatformStatus()), "店铺已被平台暂停");
    return store;
  }

  /** 返回当前用户最近一条入驻申请，不存在时返回 null。 */
  public MerchantApplication latestApplication(String openid) {
    return applications.selectLatest(openid, false);
  }

  /** 申请人响应明确剔除内部备注和联系记录。 */
  public Data publicApplication(MerchantApplication raw) {
    if (raw == null) {
      return null;
    }
    Data result = view.data(raw);
    result.remove("internalNote");
    result.remove("contactLogs");
    return result;
  }

  /** 聚合顾客、商家和平台权限，保持原 action 响应结构。 */
  public Data identity(String openid, User user) {
    PlatformUser platform = platform(openid);
    Store store = owned(openid);
    List<String> roles = user.getRoles() == null ? new ArrayList<>() : user.getRoles();
    List<String> permissions = new ArrayList<>(Arrays.asList(
        Permission.ORDER_CREATE.code(), Permission.ORDER_VIEW_SELF.code(), Permission.MERCHANT_APPLY.code()));
    if (roles.contains(UserRole.MERCHANT_OWNER.code())) {
      permissions.addAll(Arrays.asList(Permission.STORE_MANAGE_SELF.code(),
          Permission.PRODUCT_MANAGE_SELF.code(), Permission.ORDER_MANAGE_STORE.code()));
    }
    if (platform != null) {
      permissions.addAll(Arrays.asList(Permission.APPLICATION_LIST.code(), Permission.APPLICATION_CONTACT.code(),
          Permission.PAYMENT_CONFIRM.code(), Permission.STORE_VIEW_ALL.code()));
      if (oneOf(platform.getRole(), codes(PlatformRole.ADMIN, PlatformRole.SUPER))) {
        permissions.addAll(Arrays.asList(Permission.APPLICATION_APPROVE.code(), Permission.STORE_SUSPEND.code(),
            Permission.PRODUCT_MODERATE.code(), Permission.AUDIT_LOG_VIEW.code()));
      }
      if (PlatformRole.SUPER.matches(platform.getRole())) {
        permissions.addAll(Arrays.asList(Permission.PLATFORM_USER_MANAGE.code(),
            Permission.PLATFORM_CONFIG_MANAGE.code()));
      }
    }
    return of("openid", openid, "roles", roles, "platformRole", platform == null ? null : platform.getRole(),
        "permissions", permissions, "merchantApplication", publicApplication(latestApplication(openid)),
        "store", view.data(store));
  }

  /** 审计与业务更新共用外层事务，操作失败时不会留下虚假成功记录。 */
  public void audit(PlatformUser operator, String action, String type, String id,
      Object before, Object after, String reason) {
    AuditLog log = new AuditLog();
    log.setOperatorOpenid(operator.getOpenid());
    log.setOperatorRole(operator.getRole());
    log.setAction(action);
    log.setTargetType(type);
    log.setTargetId(id);
    log.setBefore(Entities.snapshot(before));
    log.setAfter(Entities.snapshot(after));
    log.setReason(reason);
    auditLogs.insert(Entities.create(log));
  }
}
