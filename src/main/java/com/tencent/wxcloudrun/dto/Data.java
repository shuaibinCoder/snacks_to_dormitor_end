package com.tencent.wxcloudrun.dto;

import com.tencent.wxcloudrun.service.BusinessException;
import java.math.BigDecimal;
import java.util.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.RECORD_ID_PREFIX;

/** 保持原 action 协议的 JSON 对象；输入只通过字段白名单写入。 */
public class Data extends LinkedHashMap<String, Object> {
  /** 创建空协议对象。 */
  public Data() {}

  /** 复制已有 Map，保持字段插入顺序；空来源按空对象处理。 */
  public Data(Map<String, ?> source) {
    if (source != null) {
      putAll(source);
    }
  }

  /** 按 key、value 成对参数构造协议对象。 */
  public static Data of(Object... pairs) {
    Data data = new Data();
    for (int i = 0; i < pairs.length; i += 2) {
      data.put((String) pairs[i], pairs[i + 1]);
    }
    return data;
  }

  /** 追加成对字段并返回当前对象，便于构造动态响应。 */
  public Data with(Object... pairs) {
    putAll(of(pairs));
    return this;
  }

  /** 读取去空白字符串，null 归一为空字符串。 */
  public String text(String key) {
    Object value = get(key);
    return value == null ? "" : String.valueOf(value).trim();
  }

  /** 读取字符串；空值使用调用方提供的默认值。 */
  public String text(String key, String fallback) {
    String value = text(key);
    return value.isEmpty() ? fallback : value;
  }

  /** 精确读取 long，拒绝小数和溢出，null 归一为 0。 */
  public long number(String key) {
    Object value = get(key);
    return value == null ? 0 : new BigDecimal(value.toString()).longValueExact();
  }

  /** 读取布尔标记，仅 true 或非零数值视为真。 */
  public boolean flag(String key) {
    Object value = get(key);
    return Boolean.TRUE.equals(value) || value instanceof Number && ((Number) value).intValue() != 0;
  }

  /** 读取对象副本，null 返回空对象，非 Map 抛出业务参数异常。 */
  @SuppressWarnings("unchecked")
  public Data object(String key) {
    Object value = get(key);
    if (value == null) {
      return new Data();
    }
    require(value instanceof Map, "参数 " + key + " 必须为对象");
    return new Data((Map<String, Object>) value);
  }

  /** 读取数组副本，null 返回空数组，非 List 抛出业务参数异常。 */
  @SuppressWarnings("unchecked")
  public List<Object> array(String key) {
    Object value = get(key);
    if (value == null) {
      return new ArrayList<>();
    }
    require(value instanceof List, "参数 " + key + " 必须为数组");
    return new ArrayList<>((List<Object>) value);
  }

  /** 读取必填字符串并校验最大字符数。 */
  public String required(String key, int max) {
    String value = text(key);
    require(!value.isEmpty() && value.length() <= max, "请正确填写 " + key);
    return value;
  }

  /** 读取范围内整数；缺省使用 fallback，小数、越界和溢出均拒绝。 */
  public long integer(String key, long fallback, long min, long max) {
    try {
      long value = containsKey(key) && get(key) != null
          ? new BigDecimal(get(key).toString()).longValueExact()
          : fallback;
      require(value >= min && value <= max, "参数 " + key + " 超出范围");
      return value;
    } catch (ArithmeticException | NumberFormatException e) {
      throw new BusinessException("参数 " + key + " 必须为整数");
    }
  }

  /** 仅复制白名单字段，未知输入不会进入后续实体转换。 */
  public Data pick(String... keys) {
    Data result = new Data();
    for (String key : keys) {
      if (containsKey(key)) {
        result.put(key, get(key));
      }
    }
    return result;
  }

  /** 判断值是否属于服务端固定选项。 */
  public static boolean oneOf(String value, String... options) {
    return Arrays.asList(options).contains(value);
  }

  /** 条件不成立时抛出带安全中文提示的业务异常。 */
  public static void require(boolean allowed, String message) {
    if (!allowed) {
      throw new BusinessException(message);
    }
  }

  /** 返回服务端当前 Unix 毫秒时间。 */
  public static long now() {
    return System.currentTimeMillis();
  }

  /** 生成与原云数据库隔离的 cr_ 业务记录 ID。 */
  public static String id() {
    return RECORD_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
  }
}
