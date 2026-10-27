package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 仅在已开启微信云托管开放接口服务的容器中使用；不携带用户提交的 URL。 */
@Component
public class WechatMessageClient {
  private final RestTemplate http;

  public WechatMessageClient(RestTemplateBuilder builder) {
    http = builder.setConnectTimeout(Duration.ofSeconds(WECHAT_CONNECT_TIMEOUT_SECONDS))
            .setReadTimeout(Duration.ofSeconds(WECHAT_READ_TIMEOUT_SECONDS)).build();
  }

  /** 调用固定微信订阅消息接口；响应缺少 errcode 时按结果未知处理。 */
  public Data send(Data payload) {
    Data response = http.postForObject(WECHAT_SUBSCRIBE_URL, payload, Data.class);
    if (response == null || !response.containsKey("errcode")) {
      throw new IllegalStateException("微信接口返回不完整，发送结果未知");
    }
    return response;
  }
}
