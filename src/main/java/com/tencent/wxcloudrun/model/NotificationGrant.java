package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 微信一次性订阅授权凭证，对应 notification_grants 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationGrant extends BusinessEntity {
  /** 授权所属用户 OpenID，绑定和领取均须匹配。 */
  private String openid;
  /** 授权对应的固定模板业务键。 */
  private String templateKey;
  /** 授权对应的订阅身份。 */
  private String subscriptionRole;
  /** ORDER_SUBMIT 等授权产生场景，本单通知必须严格匹配。 */
  private String scene;
  /** AVAILABLE、SENDING、CONSUMED、FAILED 或 UNKNOWN。 */
  private String status;
  /** 绑定业务类型，本单授权固定为 ORDER。 */
  private String boundBizType;
  /** 已绑定的订单 ID；空值表示尚未绑定业务。 */
  private String boundBizId;
  /** 微信明确成功并消费授权的时间，Unix 毫秒值；未消费为 0。 */
  private long consumedAt;
}
