package com.tencent.wxcloudrun.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.MappedTypes;
import java.util.List;

/** 用户角色和主营品类等 JSON 字符串数组的类型映射。 */
@MappedTypes(List.class)
public class StringListTypeHandler extends JacksonTypeHandler<List<String>> {
  public StringListTypeHandler() {
    super(new ObjectMapper().getTypeFactory().constructCollectionType(List.class, String.class));
  }
}
