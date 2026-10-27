package com.tencent.wxcloudrun.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dto.Data;
import java.util.List;

/** 商品快照、状态历史、联系记录和字段说明等 JSON 对象数组的类型映射。 */
public class DataListTypeHandler extends JacksonTypeHandler<List<Data>> {
  public DataListTypeHandler() {
    super(new ObjectMapper().getTypeFactory().constructCollectionType(List.class, Data.class));
  }
}
