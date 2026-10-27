package com.tencent.wxcloudrun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 云托管启动类；扫描每表 Mapper，并由 Spring Boot 装配事务和 Flyway。 */
@SpringBootApplication
@MapperScan(basePackages = {"com.tencent.wxcloudrun.dao"})
public class WxCloudRunApplication {

  /** 启动 Spring Boot HTTP 服务，Flyway 和 Mapper 由容器按运行配置装配。 */
  public static void main(String[] args) {
    SpringApplication.run(WxCloudRunApplication.class, args);
  }
}
