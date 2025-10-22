package org.example.Z_Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Markdown 消息工具类
 * 用于处理 Markdown 消息的格式化、验证和转换
 * 
 * 参考文档：https://bot.q.qq.com/wiki/develop/api-v2/server-inter/message/type/
 * 
 * @author QQ Robot Team
 * @since 2.0.0
 */
public class MarkdownMessageUtils {

  /**
   * Markdown 消息类型常量
   */
  public static final int MARKDOWN_TYPE = 2;

  /**
   * 构建 Markdown 消息对象
   * 
   * @param content       消息内容（必填）
   * @param markdownObj   Markdown 对象
   * @return 完整的消息对象
   */
  public static Map<String, Object> buildMarkdownMessageBody(
      String content, 
      Map<String, Object> markdownObj) {
    Map<String, Object> body = new HashMap<>();
    
    // content 字段为必填，即使是空字符串也要提供
    body.put("content", content != null ? content : " ");
    
    // 设置消息类型为 Markdown
    body.put("msg_type", MARKDOWN_TYPE);
    
    // 添加 Markdown 对象
    if (markdownObj != null) {
      body.put("markdown", markdownObj);
    }
    
    return body;
  }

  /**
   * 构建带有模板和参数的 Markdown 对象
   * 
   * @param templateId 模板 ID
   * @param params     模板参数
   * @return Markdown 对象
   */
  public static Map<String, Object> buildMarkdownObject(
      String templateId, 
      Map<String, String> params) {
    Map<String, Object> markdown = new HashMap<>();
    
    if (templateId != null && !templateId.isEmpty()) {
      markdown.put("template_id", templateId);
    }
    
    if (params != null && !params.isEmpty()) {
      // 将参数转换为 QQ 机器人 API 格式
      Map<String, String> convertedParams = convertParams(params);
      markdown.put("params", convertedParams);
    }
    
    return markdown;
  }

  /**
   * 转换参数格式
   * 从简单的 key-value 对转换为 QQ API 格式
   * 
   * @param params 原始参数
   * @return 转换后的参数
   */
  private static Map<String, String> convertParams(Map<String, String> params) {
    Map<String, String> converted = new HashMap<>();
    
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      
      // 对值进行转义
      String escapedValue = escapeMarkdownValue(value);
      converted.put(key, escapedValue);
    }
    
    return converted;
  }

  /**
   * 转义 Markdown 值中的特殊字符
   * 
   * @param value 原始值
   * @return 转义后的值
   */
  public static String escapeMarkdownValue(String value) {
    if (value == null) {
      return "";
    }
    
    return value
        .replace("\\", "\\\\")  // 反斜杠
        .replace("\"", "\\\"")  // 双引号
        .replace("\n", "\\n")   // 换行
        .replace("\r", "\\r")   // 回车
        .replace("\t", "\\t");  // 制表符
  }

  /**
   * 验证 Markdown 参数
   * 
   * @param templateId 模板 ID
   * @param params     参数映射
   * @return 验证结果，null 表示验证通过
   */
  public static String validateMarkdownObject(
      String templateId, 
      Map<String, String> params) {
    // 检查模板 ID
    if (templateId == null || templateId.trim().isEmpty()) {
      return "模板 ID 不能为空";
    }
    
    // 检查参数
    if (params == null || params.isEmpty()) {
      return "参数不能为空";
    }
    
    // 检查参数大小
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      
      if (key == null || key.trim().isEmpty()) {
        return "参数 key 不能为空";
      }
      
      if (value != null && value.length() > 1000) {
        return "参数 '" + key + "' 的值过长（超过 1000 字符）";
      }
    }
    
    return null;
  }

  /**
   * 构建标题风格的 Markdown 消息
   * 
   * @param title       标题
   * @param content     内容
   * @param templateId  模板 ID
   * @return 完整的消息对象
   */
  public static Map<String, Object> buildTitleMarkdownMessage(
      String title, 
      String content, 
      String templateId) {
    Map<String, String> params = new HashMap<>();
    params.put("title", title);
    params.put("content", content);
    
    Map<String, Object> markdown = buildMarkdownObject(templateId, params);
    return buildMarkdownMessageBody(" ", markdown);
  }

  /**
   * 构建列表风格的 Markdown 消息
   * 
   * @param title       列表标题
   * @param items       列表项
   * @param templateId  模板 ID
   * @return 完整的消息对象
   */
  public static Map<String, Object> buildListMarkdownMessage(
      String title, 
      java.util.List<String> items, 
      String templateId) {
    Map<String, String> params = new HashMap<>();
    params.put("title", title);
    
    // 构建列表字符串
    StringBuilder itemsStr = new StringBuilder();
    if (items != null) {
      for (int i = 0; i < items.size(); i++) {
        itemsStr.append("- ").append(items.get(i));
        if (i < items.size() - 1) {
          itemsStr.append("\n");
        }
      }
    }
    params.put("items", itemsStr.toString());
    
    Map<String, Object> markdown = buildMarkdownObject(templateId, params);
    return buildMarkdownMessageBody(" ", markdown);
  }

  /**
   * 构建表格风格的 Markdown 消息
   * 
   * @param headers     表头
   * @param rows        表行数据
   * @param templateId  模板 ID
   * @return 完整的消息对象
   */
  public static Map<String, Object> buildTableMarkdownMessage(
      String[] headers, 
      java.util.List<String[]> rows, 
      String templateId) {
    Map<String, String> params = new HashMap<>();
    
    // 构建表头
    StringBuilder table = new StringBuilder();
    if (headers != null && headers.length > 0) {
      table.append("| ");
      for (String header : headers) {
        table.append(header).append(" | ");
      }
      table.append("\n");
      
      // 分隔线
      table.append("|");
      for (int i = 0; i < headers.length; i++) {
        table.append(" --- |");
      }
      table.append("\n");
      
      // 数据行
      if (rows != null) {
        for (String[] row : rows) {
          table.append("| ");
          for (String cell : row) {
            table.append(cell).append(" | ");
          }
          table.append("\n");
        }
      }
    }
    
    params.put("table", table.toString());
    
    Map<String, Object> markdown = buildMarkdownObject(templateId, params);
    return buildMarkdownMessageBody(" ", markdown);
  }

  /**
   * 格式化 Markdown 代码块
   * 
   * @param language 编程语言
   * @param code     代码内容
   * @return 格式化后的代码块
   */
  public static String formatCodeBlock(String language, String code) {
    return String.format("```%s\n%s\n```", 
        language != null ? language : "", 
        code != null ? code : "");
  }

  /**
   * 格式化 Markdown 引用块
   * 
   * @param quote 引用文本
   * @return 格式化后的引用块
   */
  public static String formatQuote(String quote) {
    if (quote == null || quote.isEmpty()) {
      return "";
    }
    
    String[] lines = quote.split("\n");
    StringBuilder formatted = new StringBuilder();
    
    for (String line : lines) {
      formatted.append("> ").append(line).append("\n");
    }
    
    return formatted.toString();
  }

  /**
   * 格式化 Markdown 链接
   * 
   * @param text 显示文本
   * @param url  链接 URL
   * @return 格式化后的链接
   */
  public static String formatLink(String text, String url) {
    return String.format("[%s](%s)", text, url);
  }

  /**
   * 格式化 Markdown 加粗文本
   * 
   * @param text 文本
   * @return 格式化后的加粗文本
   */
  public static String formatBold(String text) {
    return "**" + text + "**";
  }

  /**
   * 格式化 Markdown 斜体文本
   * 
   * @param text 文本
   * @return 格式化后的斜体文本
   */
  public static String formatItalic(String text) {
    return "*" + text + "*";
  }

  /**
   * 格式化 Markdown 标题
   * 
   * @param level 标题级别 (1-6)
   * @param text  标题文本
   * @return 格式化后的标题
   */
  public static String formatHeading(int level, String text) {
    if (level < 1 || level > 6) {
      level = 1;
    }
    return "#".repeat(level) + " " + text;
  }

  /**
   * 获取 Markdown 消息类型
   * 
   * @return 消息类型值（2）
   */
  public static int getMarkdownType() {
    return MARKDOWN_TYPE;
  }
}
