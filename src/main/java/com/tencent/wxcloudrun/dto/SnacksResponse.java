package com.tencent.wxcloudrun.dto;

/** 独立于计数器模板的业务响应协议，成功值允许为 null。 */
public final class SnacksResponse {
  private SnacksResponse() {}

  /** 构造成功信封；data 可为对象、数组、布尔值或 null。 */
  public static Data ok(Object data) {
    return Data.of("success", true, "data", data);
  }

  /** 构造失败信封，只返回安全中文提示和稳定业务错误码。 */
  public static Data fail(String message, String code) {
    return Data.of("success", false, "message", message, "code", code);
  }
}
