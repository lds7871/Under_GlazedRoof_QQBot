package org.example.commands;

import org.springframework.stereotype.Component;

/**
 * 抽象指令基类
 * 提供指令的基础实现
 * 
 * @author QQ Robot Team
 * @since 2.0.0
 */
@Component
public abstract class BaseCommand implements Command {

  /**
   * 验证参数数量
   * 
   * @param args          参数数组
   * @param expectedCount 期望的参数数量
   * @return 如果参数数量不匹配，返回错误信息；否则返回null
   */
  protected String validateArgs(String[] args, int expectedCount) {
    if (args.length != expectedCount) {
      return "❌ 参数数量不正确\n" + getUsage();
    }
    return null;
  }

  /**
   * 验证最小参数数量
   * 
   * @param args     参数数组
   * @param minCount 最小参数数量
   * @return 如果参数数量不足，返回错误信息；否则返回null
   */
  protected String validateMinArgs(String[] args, int minCount) {
    if (args.length < minCount) {
      return "❌ 参数不足\n" + getUsage();
    }
    return null;
  }

  /**
   * 将参数数组连接成字符串
   * 
   * @param args       参数数组
   * @param startIndex 开始索引
   * @return 连接后的字符串
   */
  protected String joinArgs(String[] args, int startIndex) {
    if (startIndex >= args.length) {
      return "";
    }
    return String.join(" ", java.util.Arrays.copyOfRange(args, startIndex, args.length));
  }
}