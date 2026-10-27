package com.tencent.wxcloudrun.dao;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;

/**
 * MySQL JSON 与 Java 属性的 Jackson 转换基类。
 * 不把 JSON 当成拼接后的 SQL；写入时使用 PreparedStatement 参数。
 */
public abstract class JacksonTypeHandler<T> extends BaseTypeHandler<T> {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final JavaType type;

  /** @param type 保留 List 元素的泛型类型，避免全部退化为 Object。 */
  protected JacksonTypeHandler(JavaType type) {
    this.type = type;
  }

  /** 将 Java 值序列化为参数绑定的 JSON 字符串，转换失败抛出 SQLException。 */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, T value, JdbcType jdbcType) throws SQLException {
    try {
      ps.setString(i, JSON.writeValueAsString(value));
    } catch (Exception error) {
      throw new SQLException("业务 JSON 序列化失败", error);
    }
  }

  /** 按列名读取可空 JSON，保留目标泛型。 */
  @Override
  public T getNullableResult(ResultSet rs, String column) throws SQLException {
    return read(rs.getString(column));
  }

  /** 按结果集列序号读取可空 JSON。 */
  @Override
  public T getNullableResult(ResultSet rs, int index) throws SQLException {
    return read(rs.getString(index));
  }

  /** 按存储过程参数序号读取可空 JSON。 */
  @Override
  public T getNullableResult(CallableStatement cs, int index) throws SQLException {
    return read(cs.getString(index));
  }

  /** NULL 保持 Java null，不把数据库空值伪造成空对象或空数组。 */
  private T read(String value) throws SQLException {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return JSON.readValue(value, type);
    } catch (Exception error) {
      throw new SQLException("数据库业务 JSON 格式错误", error);
    }
  }
}
