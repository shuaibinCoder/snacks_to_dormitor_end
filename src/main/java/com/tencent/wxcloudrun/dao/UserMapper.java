package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** users 表访问接口：微信身份、角色和账号状态。 */
public interface UserMapper {
  /** 按主键读取，可选事务行锁。 */
  User selectById(@Param("id") String id, @Param("lock") boolean lock);
  /** 按唯一 OpenID 读取，身份确认时锁定以串行化同用户操作。 */
  User selectByOpenid(@Param("openid") String openid, @Param("lock") boolean lock);
  /** 插入新用户；OpenID 重复由数据库唯一键拒绝。 */
  int insert(User user);
  /** 插入用户；已存在时保留原角色和状态。 */
  int insertIgnore(User user);
  /** 修改平台停用状态。 */
  int updateStatus(@Param("id") String id, @Param("status") String status, @Param("updatedAt") long updatedAt);
  /** 商家审核后写角色，角色只能由服务端产生。 */
  int updateRoles(@Param("id") String id, @Param("roles") List<String> roles, @Param("updatedAt") long updatedAt);
}
