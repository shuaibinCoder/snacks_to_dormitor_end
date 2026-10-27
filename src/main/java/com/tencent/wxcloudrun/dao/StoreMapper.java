package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Store;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** stores 表访问接口：店铺归属、展示和营业状态。 */
public interface StoreMapper {
  /** 按店铺 ID 查询，可选事务行锁。 */
  Store selectById(@Param("id") String id, @Param("lock") boolean lock);
  /** 按唯一商家 OpenID 查询其店铺，可选事务行锁。 */
  Store selectByOwner(@Param("ownerOpenid") String ownerOpenid, @Param("lock") boolean lock);
  /** 在 SQL 内先过滤审核和平台状态，再按累计销量倒序限制。 */
  List<Store> selectPublic(@Param("query") String query, @Param("limit") int limit);
  /** 返回平台管理列表并限制数量，不做公开状态过滤。 */
  List<Store> selectAll(@Param("limit") int limit);
  /** 新增店铺，返回受影响行数。 */
  int insert(Store store);
  /** 审核通过时创建或恢复店铺资料及审批状态。 */
  int updateApproval(Store store);
  /** 商家只能编辑经营资料，不能通过此语句修改审批状态。 */
  int updateProfile(Store store);
  /** 仅修改商家营业状态，不改变平台审核状态。 */
  int updateBusinessStatus(@Param("id") String id, @Param("status") String status, @Param("updatedAt") long updatedAt);
  /** 仅修改平台治理状态，不擅自改变商家打烊状态。 */
  int updatePlatformStatus(@Param("id") String id, @Param("status") String status, @Param("updatedAt") long updatedAt);
  /** 完成订单时累加历史完成订单数。 */
  int incrementMonthlySales(@Param("id") String id, @Param("updatedAt") long updatedAt);
  /** 统计审核通过且平台未暂停的店铺数。 */
  long countActive();
}
