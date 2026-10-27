package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.NotificationTemplate;
import org.apache.ibatis.annotations.Param;

/** notification_templates 表访问接口：统一模板及买家通知策略。 */
public interface NotificationTemplateMapper {
  /** 按固定业务键读取统一模板。 */
  NotificationTemplate selectByKey(@Param("key") String key);
  /** 启动只补充缺省模板，不覆盖管理员配置。 */
  int insertIgnore(NotificationTemplate template);
  /** 管理员更新模板 ID、关键词、启用状态和通知节点。 */
  int updateConfig(NotificationTemplate template);
}
