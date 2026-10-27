package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 平台运营、管理员及超级管理员账号，对应 platform_users 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformUser extends BusinessEntity {
  /** 与 users.openid 对应的微信身份，唯一且区分大小写。 */
  private String openid;
  /** PLATFORM_OPERATOR、PLATFORM_ADMIN 或 SUPER_ADMIN。 */
  private String role;
  /** 平台内部展示姓名，不是微信昵称。 */
  private String displayName;
  /** 平台内部联系电话，可为空。 */
  private String phone;
  /** ACTIVE 生效，DISABLED 停用。 */
  private String status;
  /** 创建此平台账号的管理员 OpenID。 */
  private String createdBy;
}
