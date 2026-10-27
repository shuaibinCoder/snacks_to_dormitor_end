package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 统一订阅消息模板及发送策略，对应 notification_templates 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationTemplate extends BusinessEntity {
  /** 固定业务键 SHARED_BUSINESS_NOTICE。 */
  private String key;
  /** 模板中文名称。 */
  private String keyLabel;
  /** 模板用途及授权边界说明。 */
  private String description;
  /** 通知授权策略版本编码。 */
  private String policyVersion;
  /** 授权策略版本的中文展示名称。 */
  private String policyVersionLabel;
  /** 微信公众平台配置的模板 ID，测试环境默认为空。 */
  private String templateId;
  /** 默认关闭，管理员明确配置后才允许真实发送。 */
  private boolean enabled;
  /** developer、trial 或 formal；测试库默认 trial。 */
  private String miniprogramState;
  /** 买家微信提醒节点，只允许 ACCEPTED 或 DELIVERED。 */
  private String buyerOrderPushStage;
  /** 模板语义到实际微信关键词编号的映射。 */
  private com.tencent.wxcloudrun.dto.Data fields;
}
