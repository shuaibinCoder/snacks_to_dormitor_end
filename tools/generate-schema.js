/* 从原云函数枚举生成 Java 资源与带完整 COMMENT 的 MySQL 首次建表脚本；原云函数只读。 */
const fs = require('fs')
const path = require('path')
const root = path.resolve(__dirname, '..')
const source = process.argv[2]
if (!source) throw new Error('用法：node tools/generate-schema.js <小程序项目根目录> [--resources-only]')
const { dictionaryDocuments } = require(path.resolve(source, 'cloudfunctions/snacksApi/enums'))
const { COLLECTION_SCHEMAS } = require(path.resolve(source, 'cloudfunctions/snacksApi/schemas'))

// 类型：s 短文本，k 区分大小写的标识，t 长文本，n 整数，b 布尔，j JSON，d 评分。
const specs = {
  users: ['openid:k roles:j status:s', ['UNIQUE(openid)']],
  platform_users: ['openid:k role:s displayName:s phone:s status:s createdBy:k', ['UNIQUE(openid)']],
  merchant_applications: ['applicantOpenid:k storeName:s ownerName:s phone:s wechatContact:s businessHours:s businessCategories:j serviceDescription:t notice:t minimumOrder:n deliveryFee:n deliveryMinutes:n status:s publicMessage:t internalNote:t feeAmount:n paymentInstruction:t assignedOperatorOpenid:k contactLogs:j storeId:k approvedBy:k rejectedBy:k submittedAt:n contactedAt:n paidAt:n approvedAt:n rejectedAt:n', ['INDEX(applicantOpenid,createdAt)', 'INDEX(status,updatedAt)']],
  merchant_payments: ['applicationId:k applicantOpenid:k amount:n paymentMethod:s paymentTime:n voucherFileId:t status:s confirmedBy:k note:t', ['INDEX(applicationId,createdAt)']],
  stores: ['ownerOpenid:k applicationId:k name:s logoEmoji:s notice:t ownerName:s phone:s businessHours:s deliveryMinutes:n minimumOrder:n deliveryFee:n rating:d monthlySales:n approvalStatus:s platformStatus:s businessStatus:s approvedAt:n approvedBy:k', ['UNIQUE(ownerOpenid)', 'INDEX(approvalStatus,platformStatus,monthlySales)']],
  products: ['storeId:k name:s category:s price:n stock:n sold:n emoji:s image:t status:s version:n', ['INDEX(storeId,createdAt)']],
  orders: ['orderNo:k buyerOpenid:k sellerOpenid:k storeId:k storeName:s storeLogoEmoji:s items:j goodsAmount:n deliveryFee:n totalAmount:n contactName:s phone:s location:t deliveryMethod:s note:t buyerNotificationGrantId:k buyerNotificationAuthorized:b status:s statusHistory:j requestId:k requestHash:k', ['UNIQUE(orderNo)', 'UNIQUE(buyerOpenid,requestId)', 'INDEX(buyerOpenid,createdAt)', 'INDEX(sellerOpenid,createdAt)', 'INDEX(storeId,status,createdAt)']],
  audit_logs: ['operatorOpenid:k operatorRole:s action:s targetType:s targetId:k before:j after:j reason:t', ['INDEX(createdAt)']],
  system_dictionaries: ['type:k name:s description:t items:j version:s', ['UNIQUE(type)']],
  system_collection_schemas: ['collection:k name:s description:t fields:j version:s', ['UNIQUE(collection)']],
  notifications: ['recipientOpenid:k recipientRole:s subscriptionRole:s category:s type:s priority:s title:s summary:t statusText:s bizType:s bizId:k pagePath:t subscriptionGrantId:k requireSpecificGrant:b readStatus:s readAt:n hidden:b dedupeKey:s expireAt:n', ['UNIQUE(recipientOpenid,dedupeKey)', 'INDEX(recipientOpenid,readStatus,createdAt)']],
  notification_deliveries: ['notificationId:k recipientOpenid:k recipientRole:s subscriptionRole:s channel:s templateKey:k templateId:s subscriptionGrantId:k deliveryStatus:s attemptCount:n lastErrorCode:s lastErrorMessage:t sentAt:n', ['UNIQUE(notificationId,channel)', 'INDEX(recipientOpenid,channel,createdAt)']],
  notification_preferences: ['openid:k templateKey:k subscriptionRole:s lastPermission:s grantedCount:n lastGrantId:k lastScene:s', ['UNIQUE(openid,templateKey,subscriptionRole)']],
  notification_grants: ['openid:k templateKey:k subscriptionRole:s scene:s status:s boundBizType:s boundBizId:k consumedAt:n', ['INDEX(openid,status,createdAt)']],
  notification_templates: ['key:k keyLabel:s description:t policyVersion:s policyVersionLabel:s templateId:s enabled:b miniprogramState:s buyerOrderPushStage:s fields:j', ['UNIQUE(key)']]
}
const tableComments = {
  users: '小程序用户身份、服务端角色和账号状态。',
  platform_users: '平台运营、管理员和超级管理员授权。',
  merchant_applications: '商家入驻资料、联系过程、付款和审核状态。',
  merchant_payments: '商家入驻线下收款登记，不是在线支付流水。',
  stores: '审核通过的店铺资料、归属和经营状态。',
  products: '店铺商品、服务端价格、库存、销量和乐观锁版本。',
  orders: '零食订单、商品快照、状态历史和下单幂等信息。',
  audit_logs: '平台管理操作的不可变审计快照。',
  system_dictionaries: '状态和角色等固定编码的中文字典。',
  system_collection_schemas: '15 张 MySQL 业务表的字段中文说明。',
  notifications: '用户站内消息及微信投递业务上下文。',
  notification_deliveries: '站内和微信通道的投递状态与错误诊断。',
  notification_preferences: '用户、模板和订阅身份组合的授权汇总。',
  notification_grants: '微信一次性订阅授权的绑定、领取和消费状态。',
  notification_templates: '统一订阅消息模板及买家通知策略。'
}
const types = { s: "VARCHAR(255) NOT NULL DEFAULT ''", k: "VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT ''", t: 'TEXT', n: 'BIGINT NOT NULL DEFAULT 0', b: 'TINYINT NOT NULL DEFAULT 0', j: 'JSON', d: 'DECIMAL(4,2) NOT NULL DEFAULT 5' }
const entries = Object.entries(specs).map(([name, [columns, indexes]]) => ({ name, indexes, columns: Object.fromEntries(('_id:k ' + columns + ' createdAt:n updatedAt:n').split(' ').map(v => v.split(':'))) }))
const extraFields = {
  'products.version': { label: '商品版本号', description: '乐观锁版本；资料、库存和销量更新均递增，保存时必须提交加载版本。' },
  'orders.requestId': { label: '下单幂等键', description: '客户端结算请求键，与 buyerOpenid 共同唯一；相同内容重复请求返回原订单。' },
  'orders.requestHash': { label: '下单请求摘要', description: '服务端计算的 SHA-256，用于检测幂等键复用冲突，不返回客户端。' },
  'notification_grants.status': { label: '凭证状态编码', enumType: 'GRANT_STATUS', description: 'AVAILABLE、SENDING、CONSUMED、FAILED 或 UNKNOWN；只允许按状态机推进。' },
  'stores.monthlySales': { description: '历史累计完成订单数；保留原字段名，不代表按月重算。' },
  'notification_preferences.grantedCount': { description: '本地尚未消费的授权记录数，不代表微信侧实时剩余配额。' },
  'merchant_applications.rejectedBy': { label: '驳回操作人', description: '执行驳回的平台管理员 OpenID。' },
  'merchant_applications.rejectedAt': { label: '驳回时间', description: '申请被驳回的 Unix 毫秒时间，未驳回为 0。' },
  'stores.businessHours': { label: '经营时段', description: '店铺向顾客展示的经营时段文本。' },
  'stores.approvedAt': { label: '审核通过时间', description: '店铺最近一次审核通过的 Unix 毫秒时间。' },
  'stores.approvedBy': { label: '审核操作人', description: '最近一次执行店铺审核通过的管理员 OpenID。' },
  'orders.storeName': { label: '店铺名称快照', description: '下单时的店铺名称，后续改名不影响历史订单。' },
  'orders.storeLogoEmoji': { label: '店铺图标快照', description: '下单时的店铺 Emoji，后续修改不影响历史订单。' },
  'system_dictionaries.type': { label: '字典类型', description: '字典固定业务键，例如 ORDER_STATUS；全表唯一。' },
  'system_dictionaries.name': { label: '字典名称', description: '字典用途的中文展示名称。' },
  'system_dictionaries.description': { label: '字典说明', description: '字典编码的业务用途和适用范围。' },
  'system_dictionaries.items': { label: '字典项', description: '字典项 JSON 数组，每项包含 code、label 和 description。' },
  'system_dictionaries.version': { label: '资源版本', description: '开发期字典资源版本，由初始化服务维护。' },
  'system_collection_schemas.collection': { label: '业务表名', description: '保留原集合字段名，实际为 15 张 MySQL 业务表之一，全表唯一。' },
  'system_collection_schemas.name': { label: '业务表名称', description: '业务表用途的中文展示名称。' },
  'system_collection_schemas.description': { label: '业务表说明', description: '业务表的数据内容和使用边界。' },
  'system_collection_schemas.fields': { label: '字段说明', description: '字段名、中文标签、SQL 类型、枚举及用途组成的 JSON 数组。' },
  'system_collection_schemas.version': { label: '资源版本', description: '开发期字段说明资源版本，由初始化服务维护。' },
  'notifications.subscriptionRole': { label: '订阅身份', description: '微信发送使用的订阅身份，例如 BUYER、MERCHANT 或 PLATFORM。' },
  'notifications.readAt': { label: '阅读时间', description: '接收人阅读消息的 Unix 毫秒时间，未读为 0。' },
  'notifications.hidden': { label: '列表隐藏标记', description: 'true 表示不在接收人消息列表展示，不影响投递诊断。' },
  'notifications.expireAt': { label: '过期时间', description: '消息业务过期时间的 Unix 毫秒值。' },
  'notification_deliveries.recipientRole': { label: '接收人角色', description: '创建投递记录时的接收人业务角色快照。' },
  'notification_deliveries.subscriptionGrantId': { label: '订阅授权 ID', description: '本次微信投递实际领取的一次性授权记录 ID。' },
  'notification_preferences.lastGrantId': { label: '最近授权 ID', description: '最近一次 ACCEPT 生成的授权记录 ID，其他授权结果为空。' },
  'notification_templates.description': { label: '模板说明', description: '统一模板的用途、接收人和授权使用边界。' },
  'notification_templates.policyVersion': { label: '策略版本', description: '买家授权和发送策略的服务端版本编码。' },
  'notification_templates.policyVersionLabel': { label: '策略版本名称', description: '授权和发送策略版本的中文展示名称。' },
  'notification_templates.miniprogramState': { label: '小程序版本', description: '微信发送使用的小程序版本：developer、trial 或 formal。' }
}
const commonFields = {
  _id: { label: '记录 ID', description: 'Java 生成的 cr_ 独立记录主键，不与原云数据库 ID 混用。' },
  createdAt: { label: '创建时间', description: '记录创建时间的 Unix 毫秒值，由服务端写入。' },
  updatedAt: { label: '更新时间', description: '记录最后更新时间的 Unix 毫秒值，由服务端写入。' },
  openid: { label: '微信用户标识', description: '微信云托管可信网关注入的 OpenID，区分大小写。' }
}
function fieldMeta(table, field, type) {
  const original = ((COLLECTION_SCHEMAS[table] || {}).fields || []).find(f => f.field === field)
  const base = commonFields[field] || {}
  const label = base.label || (original && original.label) || field
  const description = base.description || (original && original.description) || `${label}；仅由云托管业务服务按字段白名单读写。`
  return { field, label, type: types[type], enumType: original && original.enumType, description, ...extraFields[`${table}.${field}`] }
}
function comment(value) {
  return String(value).replace(/[\\\u0000\r\n]/g, ' ').replace(/'/g, "''").slice(0, 1000)
}
const sql = '-- 首次初始化脚本；已应用 V1 的测试库须重建空库，不要重复执行本文件。\n' + entries.map(t => {
  const cols = Object.entries(t.columns).map(([field, type]) => {
    const meta = fieldMeta(t.name, field, type)
    return `  \`${field}\` ${types[type]} COMMENT '${comment(meta.description)}'${field === '_id' ? ' PRIMARY KEY' : ''}`
  })
  t.indexes.forEach((index, i) => {
    const [kind, fields] = index.slice(0, -1).split('(')
    cols.push(`  ${kind === 'UNIQUE' ? 'UNIQUE KEY' : 'KEY'} \`idx_${i}\` (${fields.split(',').map(f => '`' + f + '`').join(', ')})`)
  })
  return `CREATE TABLE \`${t.name}\` (\n${cols.join(',\n')}\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='${comment(tableComments[t.name])}';`
}).join('\n\n') + '\n'

const dictionaries = dictionaryDocuments().map(({ updatedAt, ...d }) => d)
const sending = { code: 'SENDING', label: '发送中', description: '已原子领取授权，等待外部发送结果；不自动重新领取。' }
const unknown = { code: 'UNKNOWN', label: '结果未知', description: '外部发送结果不确定，保留诊断且不自动重发。' }
dictionaries.find(d => d.type === 'NOTIFICATION_DELIVERY_STATUS').items.push(sending, unknown)
dictionaries.push({ _id: 'GRANT_STATUS', type: 'GRANT_STATUS', name: '订阅授权凭证状态', description: '云托管单次授权领取和消费状态。', version: 'cloudrun-v1', items: [
  { code: 'AVAILABLE', label: '可用', description: '授权可领取；绑定后仅允许用于本单。' }, sending,
  { code: 'CONSUMED', label: '已使用', description: '微信发送成功，授权已消费。' },
  { code: 'FAILED', label: '发送失败', description: '微信明确拒绝本次发送，不自动重发。' }, unknown
] })
const schemas = entries.map(t => ({
  _id: t.name,
  collection: t.name,
  name: (COLLECTION_SCHEMAS[t.name] || {}).name || t.name,
  description: tableComments[t.name],
  version: 'cloudrun-v1',
  fields: Object.entries(t.columns).map(([field, type]) => fieldMeta(t.name, field, type))
}))
function write(file, content) {
  const target = path.join(root, file)
  fs.mkdirSync(path.dirname(target), { recursive: true })
  fs.writeFileSync(target, content)
}
if (!process.argv.includes('--resources-only')) write('src/main/resources/db/migration/V1__snacks_business.sql', sql)
write('src/main/resources/snacks/dictionaries.json', JSON.stringify(dictionaries, null, 2) + '\n')
write('src/main/resources/snacks/schemas.json', JSON.stringify(schemas, null, 2) + '\n')
console.log(process.argv.includes('--resources-only') ? '已更新字典和字段说明，表结构与原云函数未修改。' : '已生成带表和字段 COMMENT 的 15 张业务表及字典资源；原云函数未修改。')
