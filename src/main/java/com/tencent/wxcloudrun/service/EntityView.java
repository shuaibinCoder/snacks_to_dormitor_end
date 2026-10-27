package com.tencent.wxcloudrun.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.BusinessEntity;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 强类型持久实体到原 action 响应的唯一转换边界。
 * 数据库内部字段先在业务服务中过滤，再交给此类补充展示标签。
 */
@Component
public class EntityView {
  private final ObjectMapper json;
  private final Labels labels;

  public EntityView(ObjectMapper json, Labels labels) {
    this.json = json;
    this.labels = labels;
  }

  /** @return 保留 _id、camelCase 和 JSON 数组的响应对象。 */
  public Data data(BusinessEntity entity) {
    return entity == null ? null : labels.decorate(entity.getClass(), json.convertValue(entity, Data.class));
  }

  /** 集合响应不逐条手写 Map，避免遗漏实体字段或中文标签。 */
  public List<Data> list(List<? extends BusinessEntity> entities) {
    return entities.stream().map(this::data).collect(Collectors.toList());
  }
}
