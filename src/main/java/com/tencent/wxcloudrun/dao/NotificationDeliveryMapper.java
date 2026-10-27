package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.NotificationDelivery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** notification_deliveries 表访问接口：通道投递的领取、跳过和最终诊断。 */
public interface NotificationDeliveryMapper {
  /** 新增通知通道记录，返回受影响行数。 */
  int insert(NotificationDelivery delivery);
  /** 按通知和通道唯一键查询，可选事务行锁。 */
  NotificationDelivery selectByNotification(@Param("notificationId") String notificationId, @Param("channel") String channel, @Param("lock") boolean lock);
  /** 只有 PENDING 可被领取，重复调用不能再次触发外部发送。 */
  int claimPending(NotificationDelivery delivery);
  /** 缺少模板或授权时记录跳过原因，不调用外部微信。 */
  int skip(@Param("id") String id, @Param("message") String message, @Param("updatedAt") long updatedAt);
  /** 仅允许从 SENDING 写入明确失败、成功或未知结果。 */
  int finish(NotificationDelivery delivery);
  /** 返回接收人指定通道的最近诊断记录并限制数量。 */
  List<NotificationDelivery> selectLatest(@Param("recipientOpenid") String recipientOpenid, @Param("channel") String channel, @Param("limit") int limit);
}
