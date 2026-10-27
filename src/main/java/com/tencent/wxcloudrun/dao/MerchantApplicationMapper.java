package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.MerchantApplication;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** merchant_applications 表访问接口：申请资料和状态机持久化。 */
public interface MerchantApplicationMapper {
  /** 按申请 ID 查询；lock 为 true 时使用事务行锁。 */
  MerchantApplication selectById(@Param("id") String id, @Param("lock") boolean lock);
  /** 重提前锁定最近申请，避免平台审核与用户重提交叉覆盖。 */
  MerchantApplication selectLatest(@Param("applicantOpenid") String applicantOpenid, @Param("lock") boolean lock);
  /** 按固定状态筛选并限制平台列表大小。 */
  List<MerchantApplication> selectByStatus(@Param("status") String status, @Param("limit") int limit);
  /** 新增完整申请实体，返回受影响行数。 */
  int insert(MerchantApplication application);
  /** 只覆盖允许重提的申请资料及提交状态。 */
  int updateSubmission(MerchantApplication application);
  /** 工作流更新必须匹配进入方法时的状态，并发操作返回 0。 */
  int updateProgress(@Param("application") MerchantApplication application, @Param("expectedStatus") String expectedStatus);
  /** 统计指定工作流状态的申请数。 */
  long countByStatus(@Param("status") String status);
}
