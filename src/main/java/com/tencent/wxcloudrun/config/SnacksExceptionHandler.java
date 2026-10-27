package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.controller.SnacksController;
import com.tencent.wxcloudrun.dto.*;
import com.tencent.wxcloudrun.service.BusinessException;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 将业务、并发和 JSON 错误转换为原协议，内部异常不泄露 SQL 或堆栈。 */
@RestControllerAdvice(assignableTypes = SnacksController.class)
public class SnacksExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(SnacksExceptionHandler.class);

  /** 业务提示保留预期 HTTP 状态和服务端错误码。 */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Data> business(BusinessException e) {
    return ResponseEntity.status(e.httpStatus).body(SnacksResponse.fail(e.getMessage(), e.code));
  }

  /** 请求体无法解析为 JSON 时返回 400，不回显原始内容。 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Data> malformed(Exception e) {
    return ResponseEntity.badRequest().body(SnacksResponse.fail("请求 JSON 格式错误", ErrorCode.INVALID_ARGUMENT.code()));
  }

  /** 唯一键或并发冲突统一返回 409，不暴露具体表和约束名。 */
  @ExceptionHandler({DuplicateKeyException.class, ConcurrencyFailureException.class})
  public ResponseEntity<Data> conflict(Exception e) {
    return ResponseEntity.status(HTTP_STATUS_CONFLICT)
        .body(SnacksResponse.fail("数据正在变化，请刷新后重试；下单重试请保留原请求编号", ErrorCode.CONFLICT.code()));
  }

  /** 未预期异常记录服务端日志，响应不包含 SQL、堆栈或内部配置。 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Data> unexpected(Exception e) {
    LOG.error("业务处理异常", e);
    return ResponseEntity.status(HTTP_STATUS_INTERNAL_SERVER_ERROR)
        .body(SnacksResponse.fail("服务暂时不可用，请稍后重试", ErrorCode.INTERNAL_ERROR.code()));
  }
}
