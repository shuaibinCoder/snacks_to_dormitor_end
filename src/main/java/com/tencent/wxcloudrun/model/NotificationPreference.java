package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户、模板和订阅身份组合的授权汇总，对应 notification_preferences 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationPreference extends BusinessEntity {
  /** 授权用户 OpenID，与模板及订阅身份组成唯一键。 */
  private String openid;
  /** 固定模板业务键。 */
  private String templateKey;
  /** BUYER 等订阅身份编码。 */
  private String subscriptionRole;
  /** 最近一次微信授权结果：ACCEPT、REJECT 或 BAN。 */
  private String lastPermission;
  /** 尚未消费的授权数量，领取发送时原子递减，不能代表微信剩余配额。 */
  private long grantedCount;
  /** 最近生成的授权记录 ID。 */
  private String lastGrantId;
  /** 最近授权产生场景。 */
  private String lastScene;
}
