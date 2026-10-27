package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.MerchantPayment;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** merchant_payments 表访问接口：线下收款凭据与确认记录。 */
public interface MerchantPaymentMapper {
  /** 按申请查询线下收款记录并限制返回数量。 */
  List<MerchantPayment> selectByApplication(@Param("applicationId") String applicationId, @Param("limit") int limit);
  /** 新增线下收款登记，返回受影响行数。 */
  int insert(MerchantPayment payment);
}
