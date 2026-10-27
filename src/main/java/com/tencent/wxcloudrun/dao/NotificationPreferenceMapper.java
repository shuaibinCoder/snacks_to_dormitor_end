package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.NotificationPreference;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** notification_preferences 表访问接口：授权汇总与最近授权结果。 */
public interface NotificationPreferenceMapper {
  /** 按用户、模板和订阅身份唯一键查询，可选事务行锁。 */
  NotificationPreference select(@Param("openid") String openid, @Param("templateKey") String templateKey, @Param("subscriptionRole") String subscriptionRole, @Param("lock") boolean lock);
  /** 返回指定用户的全部订阅偏好。 */
  List<NotificationPreference> selectByOpenid(@Param("openid") String openid);
  /** 初始化偏好；唯一键已存在时保留原授权计数。 */
  int insertIgnore(NotificationPreference preference);
  /** 更新最近授权结果、场景和授权 ID，不直接覆盖计数。 */
  int updatePermission(@Param("id") String id, @Param("permission") String permission, @Param("scene") String scene, @Param("grantId") String grantId, @Param("updatedAt") long updatedAt);
  /** 授权新增和发送领取分别原子加减，避免读取旧值覆盖。 */
  int incrementGrant(@Param("id") String id, @Param("updatedAt") long updatedAt);
  /** 领取授权时原子递减，计数为 0 时不允许变成负数。 */
  int consumeGrant(@Param("id") String id, @Param("updatedAt") long updatedAt);
  /** 微信明确拒绝时清零本地授权计数并记录拒绝结果。 */
  int clearGrants(@Param("id") String id, @Param("permission") String permission, @Param("updatedAt") long updatedAt);
}
