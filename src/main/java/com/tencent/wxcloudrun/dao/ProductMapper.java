package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.Product;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** products 表访问接口：商品资料、乐观锁和原子库存/销量。 */
public interface ProductMapper {
  /** 按商品 ID 查询，可选事务行锁。 */
  Product selectById(@Param("id") String id, @Param("lock") boolean lock);
  /** 查询店铺商品，可按上架状态过滤并限制数量。 */
  List<Product> selectByStore(@Param("storeId") String storeId, @Param("status") String status, @Param("limit") int limit);
  /** 新增商品，返回受影响行数。 */
  int insert(Product product);
  /** 资料保存必须匹配加载版本，防止旧库存覆盖新扣减。 */
  int updateDetails(@Param("product") Product product, @Param("expectedVersion") long expectedVersion);
  /** 上下架同时校验加载版本，避免覆盖并发资料修改。 */
  int updateStatus(@Param("id") String id, @Param("status") String status, @Param("expectedVersion") long expectedVersion, @Param("updatedAt") long updatedAt);
  /** 只有版本一致且库存足够才扣减；返回 0 必须回滚下单。 */
  int deductStock(@Param("id") String id, @Param("expectedVersion") long expectedVersion, @Param("quantity") long quantity, @Param("updatedAt") long updatedAt);
  /** 取消/拒单的次数由订单状态迁移约束，库存用原子加法恢复。 */
  int restoreStock(@Param("id") String id, @Param("quantity") long quantity, @Param("updatedAt") long updatedAt);
  /** 完成订单时原子增加销量并推进版本。 */
  int increaseSold(@Param("id") String id, @Param("quantity") long quantity, @Param("updatedAt") long updatedAt);
  /** 统计店铺内库存不高于阈值的上架商品数。 */
  long countLowStock(@Param("storeId") String storeId, @Param("threshold") long threshold);
}
