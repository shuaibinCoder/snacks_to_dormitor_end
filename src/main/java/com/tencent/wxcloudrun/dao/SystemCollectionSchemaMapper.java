package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.SystemCollectionSchema;
import java.util.List;

/** system_collection_schemas 表访问接口：MySQL 字段中文说明。 */
public interface SystemCollectionSchemaMapper {
  /** 返回 15 张业务表的字段中文说明。 */
  List<SystemCollectionSchema> selectAll();
  /** 启动补充缺失表说明，不覆盖已有内容。 */
  int insertIgnore(SystemCollectionSchema schema);
  /** 管理员主动同步时覆盖字段说明，返回受影响行数。 */
  int update(SystemCollectionSchema schema);
}
