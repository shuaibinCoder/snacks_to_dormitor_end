package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 店铺商品、售价及可售库存，对应 products 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Product extends BusinessEntity {
  /** 商品归属店铺 ID。 */
  private String storeId;
  /** 商品展示名称。 */
  private String name;
  /** 商家维护的商品分类文本。 */
  private String category;
  /** 服务端计价单价，整数分，禁止客户端指定订单价格。 */
  private long price;
  /** 当前可售件数；所有增减均通过事务及带约束 SQL 完成。 */
  private long stock;
  /** 已完成订单累计件数。 */
  private long sold;
  /** 无图片时展示的 Emoji。 */
  private String emoji;
  /** 原微信云存储文件 ID，不是 MySQL 图片内容。 */
  private String image;
  /** ON 上架，OFF 下架。 */
  private String status;
  /** 乐观锁版本；资料、库存和销量每次变化均递增。 */
  private long version;
}
