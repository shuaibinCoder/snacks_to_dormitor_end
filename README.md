# 宿舍补给站 Java 云托管后端
[![GitHub license](https://img.shields.io/github/license/WeixinCloud/wxcloudrun-express)](https://github.com/WeixinCloud/wxcloudrun-express)
![Maven](https://img.shields.io/badge/maven-3.9.9-green)
![Java](https://img.shields.io/badge/jdk-11-green)

基于原微信云托管模板，使用 Java 11、Spring Boot 2.5.5、MyBatis 和 MySQL 实现宿舍零食业务；保留 Flyway 依赖和脚本，但默认关闭自动迁移。Java 独立执行原云函数的 45 个 action，不转发云函数，也不运行 JavaScript。

配套小程序：`/Users/shuaibin/WeChatProjects/snacks_to_dormitory`。原 `cloudfunctions/` 和云数据库保持独立；本服务仅使用新的 MySQL 测试库，不迁移、不双写、不自动切流。商品图片暂时沿用原微信云存储，MySQL 只保存文件 ID。


## 验证环境准备

以下是人工准备和验证步骤，代码实现不代表已经部署或完成真机验收。

1. 为本服务准备独立、空的 MySQL 8 数据库，禁止填写原业务库连接：
   ```sql
   CREATE DATABASE snacks_cloudrun_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. 通过运行环境注入配置，不把真实凭据提交到代码或打入镜像：

   | 环境变量 | 含义 |
   | --- | --- |
   | `MYSQL_ADDRESS` | 独立测试 MySQL 的 `主机:端口` |
   | `MYSQL_DATABASE` | 默认 `snacks_cloudrun_test`，必须是指定测试库 |
   | `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 仅对测试库有权限的数据库账号 |
   | `WECHAT_APPID` | 唯一允许访问的小程序 AppID，不配置则拒绝业务请求 |
   | `PORT` | 默认 `80`，可覆盖；云托管端口设置需与之相同 |

3. 默认不执行任何建表、迁移或启动数据初始化：`spring.flyway.enabled=false`、`spring.sql.init.mode=never`、`snacks.initialize-on-startup=false`。`src/main/resources/db/migration/V1__snacks_business.sql` 保留作结构参考，启动不会执行；正常业务请求仍可读写数据库。
4. 不自动 baseline 非空库，不通过小程序请求建表；`initCollections` 仅兼容返回 `true`。现有库无需为启动补建 `flyway_schema_history`，也不要重复执行 V1。表结构和初始数据由维护者人工管理；管理员主动同步字典功能保留。
5. 平台人员和商家权限均需在测试库重新建立，见下方首位管理员初始化。不会把第一位访问者自动设为管理员。

### 微信云托管身份与网络前提

- 小程序通过 `wx.cloud.callContainer` 调用 `/api/snacks`，配置云托管环境和 `X-WX-SERVICE`；客户端不能提供操作者 OpenID。
- 服务只读取微信云托管调用链路自动注入的 `X-WX-SOURCE`、`X-WX-APPID`、`X-WX-OPENID`，校验 AppID、身份缺失和重复头。这与开发管理中的独立“微信网关”产品无关，无需开通后者。当前限定同一小程序，不启用跨账号资源复用，也不提供生产 mock 身份开关。
- **必须关闭公网业务入口，确保业务容器只能经微信云托管调用链路访问。** 任意公网客户端都能伪造请求头；仅设置 AppID 并不足以保证鉴权安全。部署验证时必须检查这一边界，不能为调试开放业务公网入口。
- 未来 Web 后台需要单独设计登录会话及身份验证，不能复用自填微信头作为登录。
- 参考 [微信云托管快速开始](https://developers.weixin.qq.com/miniprogram/dev/wxcloudrun/src/basic/guide.html)。

## 本地调试
下载代码在本地调试，请参考[微信云托管本地调试指南](https://developers.weixin.qq.com/miniprogram/dev/wxcloudrun/src/guide/debug/)。

当前本地暂与云托管共用 MySQL 5.7 的 `springboot_demo`，不同于上方独立库验收建议。项目根目录 `application-local.yml` 直接保存本地数据源配置，已被 Git 忽略且不打入镜像；切勿提交凭据。IDEA 使用 JDK 11，工作目录设为项目根目录，Program arguments 填 `--spring.profiles.active=local`。本地业务操作会修改共享库中的真实数据。

## 实时开发
代码变动时，不需要重新构建和启动容器，即可查看变动后的效果。请参考[微信云托管实时开发指南](https://developers.weixin.qq.com/miniprogram/dev/wxcloudrun/src/guide/debug/dev.html)

## Dockerfile最佳实践
请参考[如何提高项目构建效率](https://developers.weixin.qq.com/miniprogram/dev/wxcloudrun/src/scene/build/speed.html)

## 目录结构说明
~~~
.
├── Dockerfile                      Dockerfile 文件
├── LICENSE                         LICENSE 文件
├── README.md                       README 文件
├── container.config.json           模板部署「服务设置」初始化配置（二开请忽略）
├── mvnw                            mvnw 文件，处理mevan版本兼容问题
├── mvnw.cmd                        mvnw.cmd 文件，处理mevan版本兼容问题
├── pom.xml                         pom.xml文件
├── settings.xml                    maven 配置文件
├── springboot-cloudbaserun.iml     项目配置文件
├── tools/generate-schema.js        开发期建表、字典和字段说明生成器
└── src                             源码目录
    ├── main/java/com/tencent/wxcloudrun
    │   ├── controller              HTTP 入口
    │   ├── dto                     动态请求、响应及 JSON 快照对象
    │   ├── service                 业务事务、权限、DTO 装饰和微信调用
    │   ├── model                   15 个强类型业务实体及公共基础实体
    │   ├── dao                     每表 Mapper 接口和 Jackson TypeHandler
    │   └── config                  身份解析、异常和运行配置
    └── main/resources
        ├── mapper                  与 15 个业务 Mapper 同名的 XML/resultMap
        ├── db/migration            Flyway 建表脚本
        └── snacks                  中文字典与字段说明资源
~~~


## 业务 API

`POST /api/snacks`，JSON 请求格式为 `{ "action": "getMyIdentity" }`，其他 action 参数与原云函数一致。

```json
{ "success": true, "data": {} }
```

失败响应为 `{ "success": false, "message": "中文提示", "code": "错误码" }`。业务错误一般保留 HTTP 200；身份失败为 401/403，并发或幂等冲突为 409，内部异常为 500 且不暴露 SQL 或堆栈。未知 action 为 `UNKNOWN_ACTION`。

- `SnacksService` 显式注册身份、申请审批、收款、店铺、商品、订单、平台管理、字典和通知共 45 个 action。按 controller / dto / service / dao / model / config 分层。
- 保留 `_id`、camelCase、中文标签、毫秒时间戳及原数组、`null`、`true` 响应语义；金额为整数分，业务日按 `Asia/Shanghai` 计算。
- `createOrder` 要求 `order.requestId`（8–64 位字母、数字、下划线或连字符）。同一买家同一键和内容返回原订单；内容变化返回 `IDEMPOTENCY_CONFLICT`，不自动重试。
- 下单以服务端价格计价，合并商品并锁定库存；订单、库存和本单授权绑定同事务。取消/拒单回库存及完成计销量只执行一次。
- 编辑已有商品必须带加载时的 `product.version`，库存或资料变化会推进版本，过期保存需刷新。业务 ID 使用 `cr_` 前缀，不能当成旧云数据库 ID。
- 15 张表分别使用“强类型实体 + Mapper 接口 + 同名 XML/resultMap”。动态 `Data` 只保留在 HTTP 输入输出和 JSON 快照边界；`EntityView` 统一恢复 `_id`、camelCase 和中文标签。XML 固定表名、列名、过滤、排序和限制，参数均使用 `#{}`，不存在跨表通用 Mapper 或客户端 SQL 片段。
- 15 张表和 JSON 字段说明见 `src/main/resources/snacks/schemas.json`。`tools/generate-schema.js` 同步生成建表 COMMENT、字段说明和字典，是开发期工具，不在 Java 运行时执行，也不用于对已发布的迁移重写。

## 首位超级管理员初始化（仅测试库）

先由维护者确认业务表已准备完成，再以指定微信测试账号进入小程序的云托管版本，通过“我的 → 复制我的身份ID”获取本 AppID 下的 OpenID。由有权限的数据库维护者检查连接和 `DATABASE()` 后，手动执行下方 SQL。`@bootstrap_openid` 必须显式填写，空值不会创建账号；只允许为已访问过的 ACTIVE 用户创建首位管理员，不覆盖已有人员或自动提权。

```sql
USE snacks_cloudrun_test;
SELECT DATABASE();
SET @bootstrap_openid = ''; -- 填写指定测试人员的真实 OpenID，不能为空
SET @bootstrap_now = CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED);

INSERT INTO platform_users
  (`_id`, `openid`, `role`, `displayName`, `phone`, `status`, `createdBy`, `createdAt`, `updatedAt`)
SELECT CONCAT('cr_', REPLACE(UUID(), '-', '')), u.openid, 'SUPER_ADMIN',
       '测试平台负责人', '', 'ACTIVE', u.openid, @bootstrap_now, @bootstrap_now
FROM users u
WHERE u.openid = @bootstrap_openid AND u.status = 'ACTIVE'
  AND @bootstrap_openid REGEXP '^[a-zA-Z0-9_-]{1,64}$'
  AND NOT EXISTS (SELECT 1 FROM platform_users p WHERE p.role = 'SUPER_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM platform_users p WHERE p.openid = @bootstrap_openid);
SELECT ROW_COUNT() AS created_admin_count;
```

应创建 1 条；为 0 时检查用户、库名和已有管理员，不要改成无条件更新。重新进入“我的”确认权限，其他人员通过平台管理页添加。此 SQL 是人工初始化示例，没有对应的自助提权 API。

## 通知配置

- 测试库默认 `enabled=false`、`miniprogramState=trial`。管理员配置模板 ID、实际关键词编号及接单/送达节点后才能开启真实发送；缺配置记录跳过，不伪造成功。
- 云托管控制台开启“开放接口服务”，授权 `cgi-bin/message/subscribe/send`，并创建新服务版本使配置生效。Java 通过 `http://api.weixin.qq.com/cgi-bin/message/subscribe/send` 调用，按 HTTP API 使用 `template_id`、`miniprogram_state` 并检查 `errcode`，不依赖 `wx-server-sdk`。
- 商家/平台业务通知保留站内信；买家每单授权仅用于 `ACCEPTED` 或 `DELIVERED` 中选定的一次通知。必须是本人、BUYER、ORDER_SUBMIT 的本单 grant，不能转绑或借用历史授权。
- 站内消息及待发送记录随事务保存，微信调用在提交后执行。发送前原子领取授权；明确失败或结果未知只记录诊断，不自动重发，也不承诺外部消息严格 exactly-once。进程中断残留的 PENDING/SENDING 需人工诊断，本次未建设恢复重试队列。
- 同一 AppID 的微信授权配额不会因 MySQL 与云数据库隔离而独立；真实测试需指定人员单独授权，避免两边同时推送。

## 小程序切换与数据边界

在小程序 `miniprogram/app.js` 的 `globalData` 中配置：

```js
useCloud: true,
backendProvider: 'cloudfunction', // 默认保留原云函数；验证时改为 cloudrun
cloudRun: { env: '', service: '', path: '/api/snacks' }
```

填写独立的云托管环境 ID 和服务名后，重新编译/启动。原 `env` 仍供云函数及商品图片云存储使用，不要替换成云托管环境。缓存、待结算、授权和下单编号按后端及环境隔离；新分享带来源，旧外部链接视为云函数，不匹配只阻断并提示。

改回 `cloudfunction` 仅恢复访问原库：MySQL 测试订单、库存、角色、消息不会回写，原库也不会自动出现在 MySQL。这不是生产数据回滚方案；正式全量切换前需另行安排迁移、在途订单、上线窗口与回滚数据方案。

## 验证、打包与健康检查

```sh
mvn -B -ntp test
mvn -B -ntp package
# 需要本机 Docker 可用，启动独立 MySQL 8.0.36 容器，不连接其他数据库
mvn -B -ntp -Pmysql-it verify
```

本机 Maven 不在 PATH 时，可使用：

```sh
"/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn" -B -ntp -f /Users/shuaibin/IdeaProjects/snacks_to_dormitor_end/pom.xml package
```

- 普通 `test` 不要求 MySQL，覆盖 45 个 action 的 HTTP 契约、身份权限、参数、状态、通知，以及 15 组实体/Mapper/XML statement、JSON TypeHandler 和建表 COMMENT 结构检查。Mock 测试不能代替真实 MySQL 事务验收。
- `mysql-it` 仅对自身创建的隔离 MySQL 容器显式开启迁移和种子数据，覆盖 Flyway、JSON、并发下单/库存/状态/审核、grant 消费、统计和大小写身份；不改变本地或云端默认开关。微信客户端使用测试替身，不会发真实消息。
- 制品为 `target/springboot-wxcloudrun-1.0.jar`。构建目录 `target/` 和本地 `.idea/` 不应提交。
- 存活探针：`/actuator/health/liveness`；就绪探针：`/actuator/health/readiness`（含数据库）；综合健康：`/actuator/health`。业务端口默认 80，健康接口不返回内部详情。
- 当前本机缺少 Docker，MySQL 集成测试未执行；尚未部署，也未完成云托管/真机验收。环境准备后验证“申请 → 联系 → 收款 → 审核 → 商品 → 下单 → 履约 → 通知”，再切回云函数确认旧链路；还需检查跨后端旧分享入口、图片与订阅手势。
- 编译与运行统一使用 Java 11，Dockerfile 构建镜像为 `maven:3.9.9-eclipse-temurin-11`，运行镜像为 `eclipse-temurin:11-jre-jammy`。Spring Boot 暂保留 2.5.5；生产发布前必须另行完成依赖支持状态和安全升级评估。云端需重新构建部署新版本才能更新 Java，单纯重启旧镜像不会生效。

## 原计数器示例（默认关闭）

原示例 Controller 保留，只有显式设置 `snacks.examples-enabled=true` 才暴露 `/`、`/api/count`，且需另行准备原示例表。它不参与业务建表和初始化，以下仅是模板接口参考，不用于真实业务部署。

### `GET /api/count`

获取当前计数

#### 请求参数

无

#### 响应结果

- `code`：错误码
- `data`：当前计数值

##### 响应结果示例

```json
{
  "code": 0,
  "data": 42
}
```

#### 调用示例

```
curl https://<云托管服务域名>/api/count
```



### `POST /api/count`

更新计数，自增或者清零

#### 请求参数

- `action`：`string` 类型，枚举值
  - 等于 `"inc"` 时，表示计数加一
  - 等于 `"clear"` 时，表示计数重置（清零）

##### 请求参数示例

```
{
  "action": "inc"
}
```

#### 响应结果

- `code`：错误码
- `data`：当前计数值

##### 响应结果示例

```json
{
  "code": 0,
  "data": 42
}
```

#### 调用示例

```
curl -X POST -H 'content-type: application/json' -d '{"action": "inc"}' https://<云托管服务域名>/api/count
```

## 使用注意
数据库和微信身份配置均以上方“验证环境准备”为准。不要直接沿用模板默认数据库或为了调试跳过云托管身份与网络边界限制；缺少配置时服务或业务请求应明确失败，不自动回退到旧后端。


## License

[MIT](./LICENSE)
