package com.tencent.wxcloudrun.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dto.Data;
import org.apache.ibatis.type.MappedTypes;

/** JSON 对象与动态快照 Data 的映射，仅用于模板字段和审计/业务快照。 */
@MappedTypes(Data.class)
public class DataTypeHandler extends JacksonTypeHandler<Data> {
  public DataTypeHandler() {
    super(new ObjectMapper().constructType(Data.class));
  }
}
