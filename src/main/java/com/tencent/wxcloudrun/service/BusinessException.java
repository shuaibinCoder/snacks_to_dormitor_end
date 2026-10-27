package com.tencent.wxcloudrun.service;

import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

public class BusinessException extends RuntimeException {
  public final String code;
  public final int httpStatus;

  public BusinessException(String message) {
    this(message, ErrorCode.BUSINESS_ERROR.code(), HTTP_STATUS_OK);
  }

  public BusinessException(String message, String code, int httpStatus) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
  }
}
