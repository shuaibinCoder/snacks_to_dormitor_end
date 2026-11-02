package com.tencent.wxcloudrun.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.wxcloudrun.dto.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.io.IOException;
import java.time.Duration;
import static com.tencent.wxcloudrun.dto.Data.of;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 仅在已开启微信云托管开放接口服务的容器中使用；不携带用户提交的 URL。 */
@Component
public class WechatMessageClient {
  private final RestTemplate http;
  /** 微信外部响应采用独立 JSON 解析，不依赖服务端响应 Content-Type。 */
  private final ObjectMapper json = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  public WechatMessageClient(RestTemplateBuilder builder) {
    http = builder.setConnectTimeout(Duration.ofSeconds(WECHAT_CONNECT_TIMEOUT_SECONDS))
            .setReadTimeout(Duration.ofSeconds(WECHAT_READ_TIMEOUT_SECONDS)).build();
  }

  /** 请求固定 HTTP 接口；保留非 2xx 的微信错误码，兼容文本类型的 JSON 响应。 */
  public Data send(Data payload) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    try {
      ResponseEntity<byte[]> response = http.exchange(WECHAT_SUBSCRIBE_URL, HttpMethod.POST,
          new HttpEntity<>(payload, headers), byte[].class);
      return parseResponse(response.getBody(), response.getStatusCodeValue());
    } catch (RestClientResponseException error) {
      return parseResponse(error.getResponseBodyAsByteArray(), error.getRawStatusCode());
    } catch (RestClientException error) {
      return unknownFailure(error);
    }
  }

  /** 只接受合法整数 errcode；HTTP 错误中的零码、空对象和 HTML 都不能证明成功。 */
  private Data parseResponse(byte[] body, int status) {
    Data response = null;
    if (body != null && body.length > 0) {
      try {
        response = json.readValue(body, Data.class);
      } catch (IOException error) {
        // 不保存原始响应，避免网关 HTML 或内部错误页泄露敏感信息。
      }
    }
    boolean httpSuccess = status >= HttpStatus.OK.value() && status < HttpStatus.MULTIPLE_CHOICES.value();
    if (response != null && response.text("errcode").matches("-?\\d+")
        && (httpSuccess || !WechatResultCode.SUCCESS.matches(response.text("errcode")))) {
      return response.pick("errcode", "errmsg");
    }
    if (!httpSuccess) {
      return unknown(WechatDiagnosticCode.HTTP_ERROR_PREFIX.code() + status,
          "微信接口返回 HTTP " + status + "，未取得有效发送结果；请检查云托管开放接口服务、接口权限及网络，不自动重发");
    }
    return unknown(WechatDiagnosticCode.INVALID_RESPONSE.code(),
        "微信接口响应不是有效 JSON 或缺少整数 errcode，发送结果未知；请检查开放接口服务及容器日志，不自动重发");
  }

  /** 记录可安全展示的异常分类，不返回可能含凭据或请求内容的原始异常文本。 */
  public static Data unknownFailure(RuntimeException error) {
    if (error instanceof ResourceAccessException) {
      Throwable cause = ((ResourceAccessException) error).getMostSpecificCause();
      return unknown(WechatDiagnosticCode.NETWORK_ERROR.code(),
          "微信接口网络异常（" + cause.getClass().getSimpleName()
              + "），发送结果未知；请检查容器网络与开放接口服务，不自动重发");
    }
    return unknown(WechatDiagnosticCode.CLIENT_ERROR.code(),
        "微信发送处理异常（" + error.getClass().getSimpleName() + "），结果未知；请检查容器日志，不自动重发");
  }

  private static Data unknown(String code, String message) {
    return of("errcode", WechatResultCode.UNKNOWN.code(), "diagnosticCode", code, "errmsg", message);
  }
}
