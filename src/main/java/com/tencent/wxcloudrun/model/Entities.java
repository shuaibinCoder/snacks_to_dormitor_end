package com.tencent.wxcloudrun.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dto.Data;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import static com.tencent.wxcloudrun.constant.SnacksConstants.MAX_STRING_LENGTH;

/** 实体的 ID、时间、DTO 转换及快照工具；不承载跨表数据库访问。 */
public final class Entities {
  private static final ObjectMapper JSON = new ObjectMapper();

  private Entities() {}

  /** 将已按业务白名单筛选的 DTO 转为实体，未知客户端字段不会进入 SQL。 */
  public static <T extends BusinessEntity> T copy(Data source, Class<T> type) {
    return JSON.convertValue(source, type);
  }

  /** 将实体或已筛选 DTO 固化为审计快照，null 保持为 JSON null。 */
  public static Data snapshot(Object source) {
    return source == null ? null : source instanceof Data ? new Data((Data) source) : JSON.convertValue(source, Data.class);
  }

  /** 新增记录前生成 cr_ ID、毫秒时间及非空字符串默认值。 */
  public static <T extends BusinessEntity> T create(T entity) {
    long now = Data.now();
    if (entity.getId() == null || entity.getId().isEmpty()) {
      entity.setId(Data.id());
    }
    if (entity.getCreatedAt() == 0) {
      entity.setCreatedAt(now);
    }
    entity.setUpdatedAt(now);
    normalizeStrings(entity);
    return entity;
  }

  /** 修改记录时只采用服务端更新时间，忽略客户端提交的 updatedAt。 */
  public static <T extends BusinessEntity> T update(T entity) {
    entity.setUpdatedAt(Data.now());
    normalizeStrings(entity);
    return entity;
  }

  /* XML 的 INSERT 显式写入所有列；空字符串与 MySQL 非空默认值保持一致。 */
  private static void normalizeStrings(Object entity) {
    for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
      for (Field field : type.getDeclaredFields()) {
        if (field.getType() != String.class || Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        try {
          field.setAccessible(true);
          String value = (String) field.get(entity);
          if (value == null) {
            field.set(entity, "");
          } else {
            Data.require(value.length() <= MAX_STRING_LENGTH, "字段 " + field.getName() + " 过长");
          }
        } catch (IllegalAccessException error) {
          throw new IllegalStateException("初始化实体字符串字段失败", error);
        }
      }
    }
  }
}
