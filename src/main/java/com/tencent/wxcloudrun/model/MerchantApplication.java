package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/** 商家入驻申请、联系过程、收款状态和最终审核快照，对应 merchant_applications 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantApplication extends BusinessEntity {
  /** 申请人 OpenID，由微信云托管调用链路注入，不能由申请资料指定。 */
  private String applicantOpenid;
  /** 申请时填写的店铺名称，审核通过后复制到店铺。 */
  private String storeName;
  /** 经营者真实称呼。 */
  private String ownerName;
  /** 平台联系及店铺履约使用的手机号。 */
  private String phone;
  /** 可选的微信号等补充联系方式。 */
  private String wechatContact;
  /** 商家申报的经营时段文本。 */
  private String businessHours;
  /** 商家填写的主营品类。 */
  private List<String> businessCategories;
  /** 商品范围、配送方式等服务说明。 */
  private String serviceDescription;
  /** 申请时填写、审核后供店铺使用的公告。 */
  private String notice;
  /** 起送金额，整数分。 */
  private long minimumOrder;
  /** 配送费，整数分。 */
  private long deliveryFee;
  /** 承诺配送时长，单位为分钟。 */
  private long deliveryMinutes;
  /** SUBMITTED、CONTACTED、PAYMENT_PENDING、PAID、APPROVED 等状态编码。 */
  private String status;
  /** 申请人可见说明。 */
  private String publicMessage;
  /** 平台内部备注，不得通过申请人详情返回。 */
  private String internalNote;
  /** 入驻费用，整数分。 */
  private long feeAmount;
  /** 线下收款方式及凭证要求，不是在线支付参数。 */
  private String paymentInstruction;
  /** 当前跟进该平台人员的 OpenID。 */
  private String assignedOperatorOpenid;
  /** 平台历次电话联系记录。 */
  private List<com.tencent.wxcloudrun.dto.Data> contactLogs;
  /** 审核通过后创建的店铺 ID。 */
  private String storeId;
  /** 执行审核通过的管理员 OpenID。 */
  private String approvedBy;
  /** 执行驳回的管理员 OpenID。 */
  private String rejectedBy;
  /** 提交时间的 Unix 毫秒值。 */
  private long submittedAt;
  /** 最近成功联系时间的 Unix 毫秒值，未联系为 0。 */
  private long contactedAt;
  /** 平台确认收款时间的 Unix 毫秒值，未确认为 0。 */
  private long paidAt;
  /** 审核通过时间的 Unix 毫秒值，未通过为 0。 */
  private long approvedAt;
  /** 驳回时间的 Unix 毫秒值，未驳回为 0。 */
  private long rejectedAt;
}
