package org.example.commands;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图片消息示例指令
 * 演示如何使用 TemplateMarkdownCommand 创建图片消息
 * 
 * 指令格式：/img
 * 用法：/img [可选URL参数]
 * 
 * 使用模板 ID: 102813362_1760679605
 * 输出示例：
 * {
 *   "msg_type": 2,
 *   "markdown": {
 *     "custom_template_id": "102813362_1760679605",
 *     "params": [
 *       {"key": "ReturnImg", "values": ["https://your-image-url.com/image.png"]}
 *     ]
 *   }
 * }
 * 
 * @author QQ Robot Team
 * @since 2.0.0
 */
@Component
public class MarkDownTestCommand extends TemplateMarkdownCommand {

  /**
   * 模板 ID：102813362_1760679605
   */
  private static final String TEMPLATE_ID = "102813362_1760679605";

  /**
   * 默认示例图片 URL
   */
  private static final String DEFAULT_IMAGE_URL = "https://q.qq.com/qqbot/static/images/f3648b8001dfa331020c096a85057715.png";

  /**
   * 获取指令名称
   * 
   * @return 指令名称
   */
  @Override
  public String getName() {
    return "img";
  }

  /**
   * 获取指令描述
   * 
   * @return 指令描述
   */
  @Override
  public String getDescription() {
    return "发送图片消息示例";
  }

  /**
   * 获取指令用法说明
   * 
   * @return 用法说明
   */
  @Override
  public String getUsage() {
    return "/img [URL] - 发送图片消息，可选指定图片 URL，默认使用示例图片";
  }

  /**
   * 获取 Markdown 模板 ID
   * 
   * @return 模板 ID
   */
  @Override
  protected String getMarkdownTemplateId() {
    return TEMPLATE_ID;
  }

  /**
   * 获取 Markdown 模板参数
   * 仅返回 ReturnImg 参数用于显示图片
   * 
   * @param args 指令参数
   * @return 参数列表
   * @throws Exception 如果生成参数时出错
   */
  @Override
  protected List<Map<String, Object>> getMarkdownParams(String[] args) throws Exception {
    List<Map<String, Object>> params = new ArrayList<>();

    // 获取图片 URL
    String imageUrl = getImageUrl(args);

    // 仅返回 ReturnImg 参数
    params.add(buildMarkdownParam("ReturnImg", imageUrl));

    return params;
  }

  /**
   * 获取图片 URL
   * 如果提供了参数，使用参数中的 URL；否则使用默认示例 URL
   * 
   * @param args 指令参数
   * @return 图片 URL
   * @throws Exception 如果获取 URL 时出错
   */
  private String getImageUrl(String[] args) throws Exception {
    // 如果提供了参数，使用第一个参数作为 URL
    if (args != null && args.length > 0 && !args[0].trim().isEmpty()) {
      String customUrl = args[0].trim();
      
      // 简单验证 URL 格式
      if (customUrl.startsWith("http://") || customUrl.startsWith("https://")) {
        return customUrl;
      } else {
        throw new IllegalArgumentException("❌ 无效的 URL 格式，请使用 http:// 或 https:// 开头");
      }
    }
    
    // 没有参数时使用默认示例 URL
    return DEFAULT_IMAGE_URL;
  }
}
