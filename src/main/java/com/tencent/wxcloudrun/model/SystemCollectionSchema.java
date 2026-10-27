package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/** 管理页使用的表字段中文说明，对应 system_collection_schemas 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemCollectionSchema extends BusinessEntity {
  /** 保留原“集合”命名，实际指向 MySQL 业务表名。 */
  private String collection;
  /** 业务表中文名称。 */
  private String name;
  /** 表的用途和数据边界。 */
  private String description;
  /** 字段名称、类型、枚举及用途说明。 */
  private List<com.tencent.wxcloudrun.dto.Data> fields;
  /** 开发期字段说明资源版本。 */
  private String version;
}
