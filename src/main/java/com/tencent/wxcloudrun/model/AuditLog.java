package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 平台管理操作的不可变审计记录，对应 audit_logs 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BusinessEntity {
  /** 执行平台操作的管理员 OpenID。 */
  private String operatorOpenid;
  /** 操作发生时的平台角色快照。 */
  private String operatorRole;
  /** 固定审计动作编码，例如 APPROVE_MERCHANT_APPLICATION。 */
  private String action;
  /** 被操作资源的固定类型编码。 */
  private String targetType;
  /** 被操作资源 ID，不接受客户端任意表名。 */
  private String targetId;
  /** 操作前快照，可含 JSON 对象或 null。 */
  private com.tencent.wxcloudrun.dto.Data before;
  /** 操作后字段或结果快照，可含 JSON 对象或 null。 */
  private com.tencent.wxcloudrun.dto.Data after;
  /** 操作原因或对申请人可见的说明。 */
  private String reason;
}
