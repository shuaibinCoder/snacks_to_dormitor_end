package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/** 小程序访问者身份及平台停用状态，对应 users 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BusinessEntity {
  /** 同一 AppID 下的微信用户标识；唯一索引且区分大小写。 */
  private String openid;
  /** 服务端维护的角色编码，例如 CUSTOMER、MERCHANT_OWNER，不接受客户端直接授权。 */
  private List<String> roles;
  /** ACTIVE 允许访问，DISABLED 禁止业务操作。 */
  private String status;
}
