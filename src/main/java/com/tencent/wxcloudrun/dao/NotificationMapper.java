package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Notification;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** notifications 表访问接口：站内消息、去重和接收人已读状态。 */
public interface NotificationMapper {
  /** 按通知 ID 查询，可选事务行锁。 */
  Notification selectById(@Param("id") String id, @Param("lock") boolean lock);
  /** 按接收人和去重键读取原通知，用于安全处理唯一键冲突。 */
  Notification selectByDedupe(@Param("recipientOpenid") String recipientOpenid, @Param("dedupeKey") String dedupeKey);
  /** 唯一键冲突时保留原通知，不新增第二条站内信。 */
  int insertIgnore(Notification notification);
  /** 仅查询当前接收人的消息，支持分类、未读过滤和数量限制。 */
  List<Notification> selectList(@Param("recipientOpenid") String recipientOpenid, @Param("category") String category, @Param("unreadOnly") boolean unreadOnly, @Param("limit") int limit);
  /** 统计当前接收人指定分类的未读数，不使用列表上限截断。 */
  long countUnread(@Param("recipientOpenid") String recipientOpenid, @Param("category") String category);
  /** 只能修改当前接收人的消息。 */
  int markRead(@Param("id") String id, @Param("recipientOpenid") String recipientOpenid, @Param("readAt") long readAt, @Param("updatedAt") long updatedAt);
  /** 将当前接收人的全部未读消息标为已读。 */
  int markAllRead(@Param("recipientOpenid") String recipientOpenid, @Param("readAt") long readAt, @Param("updatedAt") long updatedAt);
}
