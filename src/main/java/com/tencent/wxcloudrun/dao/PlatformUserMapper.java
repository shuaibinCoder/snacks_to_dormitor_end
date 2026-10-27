package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.PlatformUser;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** platform_users 表访问接口：平台人员的查询和授权。 */
public interface PlatformUserMapper {
  /** 查询指定人员及有效状态，编辑时通过 lock 锁定原角色。 */
  PlatformUser selectByOpenid(@Param("openid") String openid, @Param("status") String status, @Param("lock") boolean lock);
  /** 按更新时间倒序返回平台团队。 */
  List<PlatformUser> selectAll(@Param("limit") int limit);
  /** 分页遍历有效人员，使用主键游标避免通知去重时遗漏或重复。 */
  List<PlatformUser> selectActiveAfter(@Param("afterId") String afterId, @Param("limit") int limit);
  /** 新增平台人员授权，OpenID 唯一约束防止重复账号。 */
  int insert(PlatformUser user);
  /** 只更新人员资料、角色和状态，不允许修改 OpenID。 */
  int update(PlatformUser user);
}
