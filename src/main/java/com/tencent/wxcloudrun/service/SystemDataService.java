package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 固定字典、字段说明和默认通知模板的幂等初始化。 */
@Service
public class SystemDataService implements ApplicationRunner {
  private final SystemDictionaryMapper dictionaries;
  private final SystemCollectionSchemaMapper schemas;
  private final NotificationTemplateMapper templates;
  private final Labels labels;
  /** 仅在显式开启时允许启动补数据，未配置时也保持关闭。 */
  private final boolean initializeOnStartup;

  public SystemDataService(SystemDictionaryMapper dictionaries, SystemCollectionSchemaMapper schemas,
      NotificationTemplateMapper templates, Labels labels,
      @Value("${snacks.initialize-on-startup:false}") boolean initializeOnStartup) {
    this.dictionaries = dictionaries;
    this.schemas = schemas;
    this.templates = templates;
    this.labels = labels;
    this.initializeOnStartup = initializeOnStartup;
  }

  /** 启动只补缺省数据，不覆盖管理员已保存的模板配置。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!initializeOnStartup) {
      return;
    }
    seed(false);
    NotificationTemplate template = Entities.copy(of("_id", TEMPLATE_KEY, "key", TEMPLATE_KEY,
        "keyLabel", "统一业务状态通知模板",
        "description", "购买者订单关键节点微信提醒；商家和平台使用站内信",
        "policyVersion", NOTIFICATION_POLICY_VERSION, "policyVersionLabel", "仅购买者微信推送",
        "enabled", false, "templateId", "", "miniprogramState", MiniProgramState.TRIAL.code(),
        "buyerOrderPushStage", BuyerOrderPushStage.DELIVERED.code(),
        "fields", of("titleKey", "phrase8", "statusKey", "phrase23", "timeKey", "time3", "remarkKey", "thing2")),
        NotificationTemplate.class);
    templates.insertIgnore(Entities.create(template));
  }

  /** 管理员主动同步时才覆盖字典和字段说明，不触碰订单等业务表。 */
  public void seed(boolean refresh) {
    for (Data row : labels.dictionaries) {
      SystemDictionary value = Entities.copy(row, SystemDictionary.class);
      dictionaries.insertIgnore(Entities.create(value));
      if (refresh) {
        dictionaries.update(Entities.update(value));
      }
    }
    for (Data row : labels.schemas) {
      SystemCollectionSchema value = Entities.copy(row, SystemCollectionSchema.class);
      schemas.insertIgnore(Entities.create(value));
      if (refresh) {
        schemas.update(Entities.update(value));
      }
    }
  }
}
