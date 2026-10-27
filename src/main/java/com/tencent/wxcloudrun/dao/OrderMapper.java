package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Order;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** orders 表访问接口：订单归属查询、幂等写入和状态迁移。 */
public interface OrderMapper {
  /** 按订单 ID 查询，可选事务行锁。 */
  Order selectById(@Param("id") String id, @Param("lock") boolean lock);
  /** 同一买家和 requestId 最多一条订单。 */
  Order selectByRequest(@Param("buyerOpenid") String buyerOpenid, @Param("requestId") String requestId, @Param("lock") boolean lock);
  /** 插入下单预占记录，唯一键冲突由服务层转为幂等读取。 */
  int insert(Order order);
  /** 唯一键预占成功后补全服务端计价及快照，不能改写请求摘要。 */
  int completeCreation(Order order);
  /** buyerOpenid 与 sellerOpenid 二选一，由服务层根据身份设置。 */
  List<Order> selectByOwner(@Param("buyerOpenid") String buyerOpenid, @Param("sellerOpenid") String sellerOpenid, @Param("limit") int limit);
  /** 统计店铺指定状态的订单数。 */
  long countByStoreAndStatus(@Param("storeId") String storeId, @Param("status") String status);
  /** 统计店铺从指定毫秒时间起创建的订单数。 */
  long countByStoreSince(@Param("storeId") String storeId, @Param("createdAt") long createdAt);
  /** 汇总店铺从指定毫秒时间起完成订单的总金额，单位为分。 */
  long sumCompletedSince(@Param("storeId") String storeId, @Param("createdAt") long createdAt);
  /** 仅允许从已锁定的原状态迁移，0 行表示并发操作已抢先完成。 */
  int transitionStatus(@Param("id") String id, @Param("expectedStatus") String expectedStatus, @Param("order") Order order);
}
