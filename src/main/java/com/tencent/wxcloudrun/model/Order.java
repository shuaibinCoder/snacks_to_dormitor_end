package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/** 线下结算的零食订单及下单时快照，对应 orders 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Order extends BusinessEntity {
  /** 对外订单编号，与记录 ID 分别唯一。 */
  private String orderNo;
  /** 买家 OpenID，与 requestId 组成下单幂等唯一键。 */
  private String buyerOpenid;
  /** 下单时店铺经营者 OpenID 快照。 */
  private String sellerOpenid;
  /** 商品所属店铺 ID。 */
  private String storeId;
  /** 下单时店铺名称快照。 */
  private String storeName;
  /** 下单时店铺 Emoji 快照。 */
  private String storeLogoEmoji;
  /** 下单时商品名称、单价、图片及数量快照，后续商品编辑不影响历史订单。 */
  private List<com.tencent.wxcloudrun.dto.Data> items;
  /** 商品合计金额，整数分，由服务端计算。 */
  private long goodsAmount;
  /** 下单时配送费快照，整数分。 */
  private long deliveryFee;
  /** 商品金额与配送费之和，整数分。 */
  private long totalAmount;
  /** 收货联系人称呼。 */
  private String contactName;
  /** 履约联系手机号。 */
  private String phone;
  /** 宿舍楼及寝室等送达地址。 */
  private String location;
  /** 配送方式说明。 */
  private String deliveryMethod;
  /** 买家订单备注。 */
  private String note;
  /** 本订单专用的微信订阅授权 ID，禁止用其他订单授权替代。 */
  private String buyerNotificationGrantId;
  /** 是否已为本单绑定买家授权；不代表微信已经发送成功。 */
  private boolean buyerNotificationAuthorized;
  /** PENDING、ACCEPTED、DELIVERING、DELIVERED、COMPLETED、CANCELED 或 REJECTED。 */
  private String status;
  /** 已发生的状态及中文标签，数组只能追加，不允许重复副作用。 */
  private List<com.tencent.wxcloudrun.dto.Data> statusHistory;
  /** 客户端生成的结算请求键，与 buyerOpenid 共同唯一。 */
  private String requestId;
  /** 服务端 SHA-256 请求摘要，仅用于幂等冲突判断，绝不返回客户端。 */
  private String requestHash;
}
