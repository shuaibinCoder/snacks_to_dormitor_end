package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.service.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 仅信任隔离公网入口后的微信网关；请求头不能代替网络边界。 */
@Component
public class WechatIdentityResolver {
  private final String appid;

  public WechatIdentityResolver(@Value("${snacks.wechat.appid:}") String appid) {
    this.appid = appid;
  }

  /** 校验唯一来源、AppID 和 OpenID 请求头，拒绝跨小程序身份复用。 */
  public String resolve(HttpServletRequest request) {
    String source = header(request, HEADER_WX_SOURCE);
    String app = header(request, HEADER_WX_APPID);
    String openid = header(request, HEADER_WX_OPENID);
    if (source.isEmpty() || openid.isEmpty()) {
      throw new BusinessException("请从微信小程序访问", ErrorCode.UNAUTHENTICATED.code(), HTTP_STATUS_UNAUTHORIZED);
    }
    if (appid.isEmpty() || !appid.equals(app) || request.getHeader(HEADER_WX_FROM_APPID) != null) {
      throw new BusinessException("小程序身份不匹配", ErrorCode.FORBIDDEN.code(), HTTP_STATUS_FORBIDDEN);
    }
    if (!openid.matches(OPENID_PATTERN)) {
      throw new BusinessException("用户身份不正确", ErrorCode.UNAUTHENTICATED.code(), HTTP_STATUS_UNAUTHORIZED);
    }
    return openid;
  }

  private String header(HttpServletRequest request, String name) {
    java.util.List<String> values = Collections.list(request.getHeaders(name));
    return values.size() == 1 && values.get(0) != null ? values.get(0).trim() : "";
  }
}
