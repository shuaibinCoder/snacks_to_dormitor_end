package com.tencent.wxcloudrun.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 业务实体公共字段。
 * Java 属性使用惯用的 id，序列化时恢复为原小程序协议要求的 _id；
 * 两个时间字段均为毫秒时间戳，不使用数据库隐式更新时间。
 */
public abstract class BusinessEntity {
  /** Java 云托管记录 ID，格式为 cr_ 加无连字符 UUID。 */
  @JsonProperty("_id")
  private String id;
  /** 创建时间的 Unix 毫秒值。 */
  private long createdAt;
  /** 最后一次业务修改的 Unix 毫秒值。 */
  private long updatedAt;

  /** 返回持久层记录 ID；JSON 输出名仍为 _id。 */
  public String getId() {
    return id;
  }

  /** 设置服务端生成的记录 ID，不从普通业务输入直接采信。 */
  public void setId(String id) {
    this.id = id;
  }

  /** 返回创建时间的 Unix 毫秒值。 */
  public long getCreatedAt() {
    return createdAt;
  }

  /** 设置服务端创建时间。 */
  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  /** 返回最后更新时间的 Unix 毫秒值。 */
  public long getUpdatedAt() {
    return updatedAt;
  }

  /** 设置服务端更新时间，客户端值不参与并发控制。 */
  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }
}
