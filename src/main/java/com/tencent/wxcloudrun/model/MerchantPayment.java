package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 商家入驻线下收款登记，对应 merchant_payments 表，不代表在线支付流水。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantPayment extends BusinessEntity {
  /** 所属入驻申请 ID。 */
  private String applicationId;
  /** 付款申请人 OpenID，快照保存以避免依赖申请当前状态。 */
  private String applicantOpenid;
  /** 线下登记金额，整数分。 */
  private long amount;
  /** 线下付款方式编码，例如 OFFLINE_TRANSFER。 */
  private String paymentMethod;
  /** 平台登记的付款时间，Unix 毫秒值。 */
  private long paymentTime;
  /** 可选凭证的微信云存储文件 ID。 */
  private String voucherFileId;
  /** PENDING 待确认，CONFIRMED 已确认。 */
  private String status;
  /** 确认收款的管理员 OpenID。 */
  private String confirmedBy;
  /** 平台收款核对备注，不是支付机构回执。 */
  private String note;
}
