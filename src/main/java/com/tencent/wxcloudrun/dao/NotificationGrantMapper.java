package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.NotificationGrant;
import org.apache.ibatis.annotations.Param;

/** notification_grants 表访问接口：一次性授权的归属、绑定、领取和消费。 */
public interface NotificationGrantMapper {
  /** 每次 ACCEPT 独立新增授权记录。 */
  int insert(NotificationGrant grant);
  /** 按授权 ID 查询，可选事务行锁。 */
  NotificationGrant selectById(@Param("id") String id, @Param("lock") boolean lock);
  /** 测试推送只能领取非下单场景、未绑定业务的可用授权。 */
  NotificationGrant selectAvailableForTest(@Param("openid") String openid, @Param("subscriptionRole") String subscriptionRole, @Param("templateKey") String templateKey);
  /** 本单授权必须属于本人、BUYER、ORDER_SUBMIT 且尚未绑定。 */
  int bindOrder(@Param("id") String id, @Param("openid") String openid, @Param("templateKey") String templateKey, @Param("orderId") String orderId, @Param("updatedAt") long updatedAt);
  /** 领取时再次限制身份、模板、角色、绑定订单和 AVAILABLE 状态。 */
  int claim(@Param("grant") NotificationGrant grant, @Param("specific") boolean specific, @Param("orderId") String orderId);
  /** 只有已领取的 SENDING 记录才能进入最终状态。 */
  int finish(@Param("id") String id, @Param("status") String status, @Param("consumedAt") long consumedAt, @Param("updatedAt") long updatedAt);
}
