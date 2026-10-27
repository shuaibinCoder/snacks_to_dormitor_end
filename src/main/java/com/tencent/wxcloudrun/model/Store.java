package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/** 审核通过的商家店铺，对应 stores 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Store extends BusinessEntity {
  /** 店铺所属商家 OpenID，一名商家最多一家店。 */
  private String ownerOpenid;
  /** 来源入驻申请 ID。 */
  private String applicationId;
  /** 对顾客展示的店铺名称。 */
  private String name;
  /** 店铺头像使用的 Emoji 文本。 */
  private String logoEmoji;
  /** 店铺公告，允许为空字符串。 */
  private String notice;
  /** 店主展示称呼。 */
  private String ownerName;
  /** 顾客或平台联系店铺的手机号。 */
  private String phone;
  /** 对顾客展示的经营时段文本。 */
  private String businessHours;
  /** 预计配送分钟数。 */
  private long deliveryMinutes;
  /** 起送金额，整数分。 */
  private long minimumOrder;
  /** 配送费，整数分。 */
  private long deliveryFee;
  /** 当前展示评分；本项目尚无评价系统，新店保留原默认值。 */
  private BigDecimal rating;
  /** 历史累计完成订单数；沿用了原 monthlySales 字段名，不代表按月重算。 */
  private long monthlySales;
  /** APPROVED 才允许公开经营。 */
  private String approvalStatus;
  /** ACTIVE 展示，SUSPENDED 平台暂停。 */
  private String platformStatus;
  /** OPEN 营业，CLOSED 打烊；与平台状态分别控制。 */
  private String businessStatus;
  /** 最近审核通过时间的 Unix 毫秒值。 */
  private long approvedAt;
  /** 最近执行审核通过的管理员 OpenID。 */
  private String approvedBy;
}
