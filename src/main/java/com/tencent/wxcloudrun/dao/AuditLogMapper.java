package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.AuditLog;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** audit_logs 表访问接口：审计记录只新增，不提供业务修改或删除。 */
public interface AuditLogMapper {
  /** 追加不可变审计记录，返回受影响行数。 */
  int insert(AuditLog log);
  /** 按创建时间倒序返回最近审计记录并限制数量。 */
  List<AuditLog> selectRecent(@Param("limit") int limit);
}
