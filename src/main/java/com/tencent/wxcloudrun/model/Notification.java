package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户站内消息及可选微信投递的业务上下文，对应 notifications 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Notification extends BusinessEntity {
  /** 消息接收人的 OpenID，已读更新必须匹配此归属。 */
  private String recipientOpenid;
  /** 接收人的业务角色快照。 */
  private String recipientRole;
  /** 微信订阅身份，买家为 BUYER，商家或平台按模板约定。 */
  private String subscriptionRole;
  /** 消息分类编码，用于列表筛选和未读统计。 */
  private String category;
  /** 业务消息类型编码，例如 ORDER_DELIVERED。 */
  private String type;
  /** 消息优先级编码。 */
  private String priority;
  /** 站内消息标题。 */
  private String title;
  /** 消息摘要及微信 remark 来源。 */
  private String summary;
  /** 订单等业务的短状态文本。 */
  private String statusText;
  /** 关联业务资源类型。 */
  private String bizType;
  /** 关联业务资源 ID，本单授权校验使用订单 ID。 */
  private String bizId;
  /** 小程序页面路径，云托管通知会追加后端来源参数。 */
  private String pagePath;
  /** 指定使用的授权 ID；空值仅允许非特定业务测试通知。 */
  private String subscriptionGrantId;
  /** true 表示只能使用本订单绑定的授权，不能从历史授权补发。 */
  private boolean requireSpecificGrant;
  /** UNREAD 或 READ。 */
  private String readStatus;
  /** 阅读时间的 Unix 毫秒值，未读为 0。 */
  private long readAt;
  /** 是否在接收人列表隐藏，不影响投递诊断。 */
  private boolean hidden;
  /** 同一接收人内唯一，防止业务重入产生重复站内消息。 */
  private String dedupeKey;
  /** 过期时间的 Unix 毫秒值。 */
  private long expireAt;
}
