package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.WechatIdentityResolver;
import com.tencent.wxcloudrun.dto.*;
import com.tencent.wxcloudrun.service.SnacksService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/** 原云函数 action 协议的云托管 HTTP 入口。 */
@RestController
public class SnacksController {
  private final SnacksService service;
  private final WechatIdentityResolver identity;

  public SnacksController(SnacksService service, WechatIdentityResolver identity) {
    this.service = service;
    this.identity = identity;
  }

  /** 从微信云托管调用链路解析身份，再以统一成功/失败信封返回 action 结果。 */
  @PostMapping(value = "/api/snacks", consumes = "application/json", produces = "application/json")
  public Data execute(@RequestBody Data event, HttpServletRequest request) {
    return SnacksResponse.ok(service.execute(event, identity.resolve(request)));
  }
}
