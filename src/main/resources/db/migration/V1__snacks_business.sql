-- 首次初始化脚本；已应用 V1 的测试库须重建空库，不要重复执行本文件。
CREATE TABLE `users` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '微信云托管可信网关注入的 OpenID，区分大小写。',
  `roles` JSON COMMENT '用户拥有的普通业务角色编码数组。',
  `status` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '账号是否可以正常使用。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序用户身份、服务端角色和账号状态。';

CREATE TABLE `platform_users` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '微信云托管可信网关注入的 OpenID，区分大小写。',
  `role` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '决定平台后台管理权限。',
  `displayName` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '后台展示的运营或管理员姓名。',
  `phone` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '平台人员联系电话，可选。',
  `status` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '平台权限是否启用。',
  `createdBy` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '创建该平台人员记录的管理员OPENID。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台运营、管理员和超级管理员授权。';

CREATE TABLE `merchant_applications` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `applicantOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '提交商家申请的微信用户OPENID。',
  `storeName` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '审核通过后使用的店铺名称。',
  `ownerName` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '申请人填写的真实经营者姓名。',
  `phone` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '平台电话联系申请人使用。',
  `wechatContact` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '申请人补充的微信联系方式，可选。',
  `businessHours` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '店铺计划营业时间，例如19:00-00:00。',
  `businessCategories` JSON COMMENT '申请人计划销售的商品类型。',
  `serviceDescription` TEXT COMMENT '例如本栋宿舍配送。',
  `notice` TEXT COMMENT '审核通过后作为店铺公告初始内容。',
  `minimumOrder` BIGINT NOT NULL DEFAULT 0 COMMENT '单位为分，例如1000表示10元。',
  `deliveryFee` BIGINT NOT NULL DEFAULT 0 COMMENT '单位为分。',
  `deliveryMinutes` BIGINT NOT NULL DEFAULT 0 COMMENT '商家预计完成配送需要的分钟数。',
  `status` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '申请当前所处的审批阶段。',
  `publicMessage` TEXT COMMENT '申请人在小程序中看到的状态说明。',
  `internalNote` TEXT COMMENT '只供平台人员查看，不向申请人展示。',
  `feeAmount` BIGINT NOT NULL DEFAULT 0 COMMENT '线下入驻费用，单位为分。',
  `paymentInstruction` TEXT COMMENT '平台向申请人展示的付款说明。',
  `assignedOperatorOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '负责联系和跟进该申请的平台人员。',
  `contactLogs` JSON COMMENT '记录每次联系结果、中文名称、备注、操作人和时间。',
  `storeId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '审核通过后创建或激活的店铺ID。',
  `approvedBy` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '最终审核通过的管理员OPENID。',
  `rejectedBy` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '执行驳回的平台管理员 OpenID。',
  `submittedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '申请正式提交的毫秒时间戳。',
  `contactedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '平台完成电话联系的毫秒时间戳。',
  `paidAt` BIGINT NOT NULL DEFAULT 0 COMMENT '平台确认收到线下付款的时间。',
  `approvedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '管理员审核通过的时间。',
  `rejectedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '申请被驳回的 Unix 毫秒时间，未驳回为 0。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  KEY `idx_0` (`applicantOpenid`, `createdAt`),
  KEY `idx_1` (`status`, `updatedAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家入驻资料、联系过程、付款和审核状态。';

CREATE TABLE `merchant_payments` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `applicationId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '该付款所属的入驻申请。',
  `applicantOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '付款商家的微信用户OPENID。',
  `amount` BIGINT NOT NULL DEFAULT 0 COMMENT '单位为分。',
  `paymentMethod` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '线下付款方式。',
  `paymentTime` BIGINT NOT NULL DEFAULT 0 COMMENT '商家实际付款的毫秒时间戳。',
  `voucherFileId` TEXT COMMENT '云存储中的付款凭证，可选。',
  `status` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '付款记录是否已经核实。',
  `confirmedBy` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '确认收到线下付款的平台人员OPENID。',
  `note` TEXT COMMENT '平台人员填写的付款说明。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  KEY `idx_0` (`applicationId`, `createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家入驻线下收款登记，不是在线支付流水。';

CREATE TABLE `stores` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `ownerOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '店铺所属微信用户OPENID。',
  `applicationId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '创建该店铺的商家入驻申请ID。',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '学生搜索和店铺页面展示的名称。',
  `logoEmoji` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'MVP使用的店铺Emoji图标。',
  `notice` TEXT COMMENT '学生进入店铺时展示的公告。',
  `ownerName` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '店铺经营者姓名。',
  `phone` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '处理订单和平台联系使用。',
  `businessHours` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '店铺向顾客展示的经营时段文本。',
  `deliveryMinutes` BIGINT NOT NULL DEFAULT 0 COMMENT '预计完成配送的分钟数。',
  `minimumOrder` BIGINT NOT NULL DEFAULT 0 COMMENT '单位为分。',
  `deliveryFee` BIGINT NOT NULL DEFAULT 0 COMMENT '单位为分。',
  `rating` DECIMAL(4,2) NOT NULL DEFAULT 5 COMMENT '店铺综合评分。',
  `monthlySales` BIGINT NOT NULL DEFAULT 0 COMMENT '历史累计完成订单数；保留原字段名，不代表按月重算。',
  `approvalStatus` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '店铺是否通过平台审核。',
  `platformStatus` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '店铺是否被平台暂停或封禁。',
  `businessStatus` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商家当前是否营业。',
  `approvedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '店铺最近一次审核通过的 Unix 毫秒时间。',
  `approvedBy` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '最近一次执行店铺审核通过的管理员 OpenID。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`ownerOpenid`),
  KEY `idx_1` (`approvalStatus`, `platformStatus`, `monthlySales`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核通过的店铺资料、归属和经营状态。';

CREATE TABLE `products` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `storeId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '商品所属店铺。',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '学生端展示的商品名称。',
  `category` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '例如零食、饮料、泡面或日用。',
  `price` BIGINT NOT NULL DEFAULT 0 COMMENT '单位为分，例如400表示4元。',
  `stock` BIGINT NOT NULL DEFAULT 0 COMMENT '当前可下单商品数量。',
  `sold` BIGINT NOT NULL DEFAULT 0 COMMENT '完成订单后累计增加的销售数量。',
  `emoji` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '没有上传图片时使用的Emoji图标。',
  `image` TEXT COMMENT '云存储文件ID或本地体验图片路径。',
  `status` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品是否上架。',
  `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本；资料、库存和销量更新均递增，保存时必须提交加载版本。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  KEY `idx_0` (`storeId`, `createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺商品、服务端价格、库存、销量和乐观锁版本。';

CREATE TABLE `orders` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `orderNo` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '向用户和商家展示的业务订单编号。',
  `buyerOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '下单微信用户OPENID。',
  `sellerOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '接收订单的店主OPENID。',
  `storeId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '订单所属店铺。',
  `storeName` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '下单时的店铺名称，后续改名不影响历史订单。',
  `storeLogoEmoji` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '下单时的店铺 Emoji，后续修改不影响历史订单。',
  `items` JSON COMMENT '下单时商品名称、单价、数量和图片的快照。',
  `goodsAmount` BIGINT NOT NULL DEFAULT 0 COMMENT '不含配送费，单位为分。',
  `deliveryFee` BIGINT NOT NULL DEFAULT 0 COMMENT '单位为分。',
  `totalAmount` BIGINT NOT NULL DEFAULT 0 COMMENT '双方线下结算金额，单位为分。',
  `contactName` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '订单履约联系人。',
  `phone` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商家配送联系使用。',
  `location` TEXT COMMENT '宿舍房间或约定交付位置。',
  `deliveryMethod` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '送到门口、公共区域、一楼或自取。',
  `note` TEXT COMMENT '购买者提交的订单补充说明。',
  `buyerNotificationGrantId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '仅使用下单时明确确认产生的单次授权凭证。',
  `buyerNotificationAuthorized` TINYINT NOT NULL DEFAULT 0 COMMENT 'true表示购买者明确同意本单微信提醒。',
  `status` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '订单当前履约阶段。',
  `statusHistory` JSON COMMENT '记录每次状态变化的编码、中文名称和时间。',
  `requestId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '客户端结算请求键，与 buyerOpenid 共同唯一；相同内容重复请求返回原订单。',
  `requestHash` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '服务端计算的 SHA-256，用于检测幂等键复用冲突，不返回客户端。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`orderNo`),
  UNIQUE KEY `idx_1` (`buyerOpenid`, `requestId`),
  KEY `idx_2` (`buyerOpenid`, `createdAt`),
  KEY `idx_3` (`sellerOpenid`, `createdAt`),
  KEY `idx_4` (`storeId`, `status`, `createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='零食订单、商品快照、状态历史和下单幂等信息。';

CREATE TABLE `audit_logs` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `operatorOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '执行平台操作的微信用户OPENID。',
  `operatorRole` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '操作发生时操作者的平台角色。',
  `action` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '平台执行了什么操作。',
  `targetType` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '被操作的数据类型。',
  `targetId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '被操作记录的ID。',
  `before` JSON COMMENT '关键操作发生前的数据快照。',
  `after` JSON COMMENT '关键操作发生后的数据摘要。',
  `reason` TEXT COMMENT '管理员填写的原因或备注。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  KEY `idx_0` (`createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台管理操作的不可变审计快照。';

CREATE TABLE `system_dictionaries` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `type` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '字典固定业务键，例如 ORDER_STATUS；全表唯一。',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '字典用途的中文展示名称。',
  `description` TEXT COMMENT '字典编码的业务用途和适用范围。',
  `items` JSON COMMENT '字典项 JSON 数组，每项包含 code、label 和 description。',
  `version` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '开发期字典资源版本，由初始化服务维护。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='状态和角色等固定编码的中文字典。';

CREATE TABLE `system_collection_schemas` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `collection` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '保留原集合字段名，实际为 15 张 MySQL 业务表之一，全表唯一。',
  `name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '业务表用途的中文展示名称。',
  `description` TEXT COMMENT '业务表的数据内容和使用边界。',
  `fields` JSON COMMENT '字段名、中文标签、SQL 类型、枚举及用途组成的 JSON 数组。',
  `version` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '开发期字段说明资源版本，由初始化服务维护。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`collection`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='15 张 MySQL 业务表的字段中文说明。';

CREATE TABLE `notifications` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `recipientOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '消息接收人的微信OPENID。',
  `recipientRole` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '接收消息时用户所处角色。',
  `subscriptionRole` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '微信发送使用的订阅身份，例如 BUYER、MERCHANT 或 PLATFORM。',
  `category` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '订单、商家入驻、店铺、平台或系统消息。',
  `type` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '具体触发消息的业务事件。',
  `priority` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '消息紧急程度。',
  `title` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '消息列表展示标题。',
  `summary` TEXT COMMENT '消息列表和订阅消息展示摘要。',
  `statusText` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '统一微信模板中的简短状态文字。',
  `bizType` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '消息关联的业务类型。',
  `bizId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '关联订单、申请或店铺ID。',
  `pagePath` TEXT COMMENT '点击消息后进入的小程序页面路径。',
  `subscriptionGrantId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '微信推送必须匹配当前订单绑定的授权凭证。',
  `requireSpecificGrant` TINYINT NOT NULL DEFAULT 0 COMMENT 'true时禁止使用历史遗留授权。',
  `readStatus` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '消息是否已经查看。',
  `readAt` BIGINT NOT NULL DEFAULT 0 COMMENT '接收人阅读消息的 Unix 毫秒时间，未读为 0。',
  `hidden` TINYINT NOT NULL DEFAULT 0 COMMENT 'true 表示不在接收人消息列表展示，不影响投递诊断。',
  `dedupeKey` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '防止同一业务事件重复创建消息。',
  `expireAt` BIGINT NOT NULL DEFAULT 0 COMMENT '消息业务过期时间的 Unix 毫秒值。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`recipientOpenid`, `dedupeKey`),
  KEY `idx_1` (`recipientOpenid`, `readStatus`, `createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户站内消息及微信投递业务上下文。';

CREATE TABLE `notification_deliveries` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `notificationId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '对应notifications记录ID。',
  `recipientOpenid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '消息接收人的微信OPENID。',
  `recipientRole` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '创建投递记录时的接收人业务角色快照。',
  `subscriptionRole` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '本次推送使用购买者、商家、申请人、平台或通用授权。',
  `channel` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '站内或微信订阅消息。',
  `templateKey` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '当前统一使用SHARED_BUSINESS_NOTICE。',
  `templateId` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '公众平台配置的订阅消息模板ID。',
  `subscriptionGrantId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '本次微信投递实际领取的一次性授权记录 ID。',
  `deliveryStatus` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '发送成功、失败或跳过。',
  `attemptCount` BIGINT NOT NULL DEFAULT 0 COMMENT '调用消息通道的次数。',
  `lastErrorCode` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '微信接口返回的错误码。',
  `lastErrorMessage` TEXT COMMENT '发送失败或跳过原因。',
  `sentAt` BIGINT NOT NULL DEFAULT 0 COMMENT '发送成功毫秒时间戳。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`notificationId`, `channel`),
  KEY `idx_1` (`recipientOpenid`, `channel`, `createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内和微信通道的投递状态与错误诊断。';

CREATE TABLE `notification_preferences` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '微信云托管可信网关注入的 OpenID，区分大小写。',
  `templateKey` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '统一业务状态通知模板键。',
  `subscriptionRole` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '授权次数归属于购买者、商家、申请人、平台或通用身份。',
  `lastPermission` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '最近一次请求订阅的结果。',
  `grantedCount` BIGINT NOT NULL DEFAULT 0 COMMENT '本地尚未消费的授权记录数，不代表微信侧实时剩余配额。',
  `lastGrantId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '最近一次 ACCEPT 生成的授权记录 ID，其他授权结果为空。',
  `lastScene` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '下单、营业、提交申请或平台审批。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`openid`, `templateKey`, `subscriptionRole`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户、模板和订阅身份组合的授权汇总。';

CREATE TABLE `notification_grants` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `openid` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '微信云托管可信网关注入的 OpenID，区分大小写。',
  `templateKey` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '统一业务状态通知模板键。',
  `subscriptionRole` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '当前仅订单购买者使用BUYER。',
  `scene` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '例如ORDER_SUBMIT，表示用户在提交订单时确认。',
  `status` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'AVAILABLE、SENDING、CONSUMED、FAILED 或 UNKNOWN；只允许按状态机推进。',
  `boundBizType` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '当前为ORDER。',
  `boundBizId` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '本次授权只允许用于该订单。',
  `consumedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '微信消息发送成功后记录的时间。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  KEY `idx_0` (`openid`, `status`, `createdAt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信一次性订阅授权的绑定、领取和消费状态。';

CREATE TABLE `notification_templates` (
  `_id` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' PRIMARY KEY,
  `key` VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT '统一使用SHARED_BUSINESS_NOTICE。',
  `keyLabel` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '模板用途中文说明。',
  `description` TEXT COMMENT '统一模板的用途、接收人和授权使用边界。',
  `policyVersion` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '买家授权和发送策略的服务端版本编码。',
  `policyVersionLabel` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '授权和发送策略版本的中文展示名称。',
  `templateId` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '从微信公众平台订阅消息模板库取得。',
  `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '只有开启且配置模板ID后才发送微信订阅消息。',
  `miniprogramState` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '微信发送使用的小程序版本：developer、trial 或 formal。',
  `buyerOrderPushStage` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '选择商家接单或商品送达时消耗一次购买者微信授权。',
  `fields` JSON COMMENT '标题、状态、时间和备注对应的微信模板关键词。',
  `createdAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录创建时间的 Unix 毫秒值，由服务端写入。',
  `updatedAt` BIGINT NOT NULL DEFAULT 0 COMMENT '记录最后更新时间的 Unix 毫秒值，由服务端写入。',
  UNIQUE KEY `idx_0` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一订阅消息模板及买家通知策略。';
