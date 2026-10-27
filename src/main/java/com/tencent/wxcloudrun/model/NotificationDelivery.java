package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 单条消息在站内或微信通道的发送记录，对应 notification_deliveries 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationDelivery extends BusinessEntity {
  /** 所属通知 ID，与 channel 共同唯一。 */
  private String notificationId;
  /** 接收人 OpenID 快照，用于诊断筛选。 */
  private String recipientOpenid;
  /** 接收人业务角色快照。 */
  private String recipientRole;
  /** 本次通道使用的订阅身份。 */
  private String subscriptionRole;
  /** IN_APP 或 WECHAT_SUBSCRIBE，不能用客户端字符串生成表名。 */
  private String channel;
  /** 固定模板业务键。 */
  private String templateKey;
  /** 领取发送时的微信模板 ID 快照。 */
  private String templateId;
  /** 本次实际领取的授权 ID。 */
  private String subscriptionGrantId;
  /** PENDING、SENDING、SUCCESS、FAILED_FINAL、UNKNOWN 或 SKIPPED。 */
  private String deliveryStatus;
  /** 实际领取次数；本阶段不自动重试，成功后不允许再次领取。 */
  private long attemptCount;
  /** 最近一次微信错误码或 UNKNOWN，成功时为空。 */
  private String lastErrorCode;
  /** 截断后的外部错误说明，成功时为空。 */
  private String lastErrorMessage;
  /** 微信明确成功时间的 Unix 毫秒值，未成功为 0。 */
  private long sentAt;
}
