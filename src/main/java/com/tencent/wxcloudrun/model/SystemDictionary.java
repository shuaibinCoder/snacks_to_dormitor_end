package com.tencent.wxcloudrun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/** 角色、状态等固定编码的中文字典，对应 system_dictionaries 表。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemDictionary extends BusinessEntity {
  /** 字典业务键，例如 ORDER_STATUS，与记录 ID 相同且唯一。 */
  private String type;
  /** 字典中文名称。 */
  private String name;
  /** 字典用途和编码适用范围。 */
  private String description;
  /** 字典项对象数组，每项包含 code、label、description。 */
  private List<com.tencent.wxcloudrun.dto.Data> items;
  /** 开发期字典资源版本，不由客户端修改。 */
  private String version;
}
