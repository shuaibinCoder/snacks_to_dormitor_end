package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.SystemDictionary;
import java.util.List;

/** system_dictionaries 表访问接口：固定字典的幂等初始化与管理员同步。 */
public interface SystemDictionaryMapper {
  /** 返回全部固定字典及其 JSON 字典项。 */
  List<SystemDictionary> selectAll();
  /** 启动补缺；已有字典不被默认资源覆盖。 */
  int insertIgnore(SystemDictionary dictionary);
  /** 仅管理员主动同步时覆盖字典内容，不改变业务数据。 */
  int update(SystemDictionary dictionary);
}
