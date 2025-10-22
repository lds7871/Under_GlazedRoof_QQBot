package org.example.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板 Markdown 指令基类
 * 用于支持 QQ Bot API v2 中的模板 Markdown 消息格式
 * 
 * 官方文档：https://bot.q.qq.com/wiki/develop/api-v2/server-inter/message/type/markdown.html
 * 
 * 模板 Markdown 消息格式：
 * {
 *   "msg_type": 2,
 *   "markdown": {
 *     "custom_template_id": "101993071_1658748972",
 *     "params": [
 *       {
 *         "key": "title",
 *         "values": ["标题"]
 *       },
 *       {
 *         "key": "image",
 *         "values": ["https://example.com/image.png"]
 *       }
 *     ]
 *   }
 * }
 * 
 * @author QQ Robot Team
 * @since 2.0.0
 */
@Component
public abstract class TemplateMarkdownCommand extends BaseCommand {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * 获取 Markdown 模板 ID
   * 子类应该实现此方法以返回模板 ID
   * 
   * @return 模板 ID
   */
  protected abstract String getMarkdownTemplateId();

  /**
   * 获取 Markdown 模板参数
   * 子类应该实现此方法以返回模板参数
   * 
   * @param args 指令参数
   * @return 参数列表
   * @throws Exception 如果生成参数时出错
   */
  protected abstract List<Map<String, Object>> getMarkdownParams(String[] args) throws Exception;

  /**
   * 构建模板参数项
   * 
   * @param key    模板变量名称（如 "title"）
   * @param values 变量值数组
   * @return 参数项
   */
  protected Map<String, Object> buildMarkdownParam(String key, String... values) {
    Map<String, Object> param = new HashMap<>();
    param.put("key", key);
    
    List<String> valueList = new ArrayList<>();
    for (String value : values) {
      if (value != null) {
        valueList.add(value);
      }
    }
    
    param.put("values", valueList);
    return param;
  }

  /**
   * 构建模板 Markdown 消息
   * 
   * @param params 参数列表
   * @return JSON 格式的消息对象字符串
   * @throws Exception 如果构建消息时出错
   */
  protected String buildTemplateMarkdownMessage(List<Map<String, Object>> params) throws Exception {
    String templateId = getMarkdownTemplateId();
    
    if (templateId == null || templateId.trim().isEmpty()) {
      throw new IllegalArgumentException("Markdown 模板 ID 不能为空");
    }
    
    if (params == null || params.isEmpty()) {
      throw new IllegalArgumentException("Markdown 模板参数不能为空");
    }

    // 构建 Markdown 对象
    Map<String, Object> markdown = new HashMap<>();
    markdown.put("custom_template_id", templateId);
    markdown.put("params", params);

    // 构建完整的消息体
    Map<String, Object> messageBody = new HashMap<>();
    messageBody.put("msg_type", 2);   // 2 表示 Markdown 消息类型
    messageBody.put("markdown", markdown);

    return objectMapper.writeValueAsString(messageBody);
  }

  /**
   * 执行指令
   * 
   * @param args 指令参数
   * @return JSON 格式的消息对象字符串
   * @throws Exception 如果执行过程中出错
   */
  @Override
  public String execute(String[] args) throws Exception {
    List<Map<String, Object>> params = getMarkdownParams(args);
    return buildTemplateMarkdownMessage(params);
  }
}
