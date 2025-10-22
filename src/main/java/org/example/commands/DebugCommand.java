package org.example.commands;

import org.example.debug.Debug_PortMain;
import org.example.debug.Debug_ping;
import org.springframework.stereotype.Component;

/**
 * 调试命令 - 用于测试消息处理和AI服务
 */
@Component
public class DebugCommand extends BaseCommand {

  @Override
  public String getName() {
    return "debug";
  }

  @Override
  public String getDescription() {
    return "选择:[ping][port]，测试服务器功能";
  }

  @Override
  public String getUsage() {
    return "用法：\n" +
        "/debug ping -读取主机服务状态\n" +
        "/debug port -主机端口占用情况\n"
    ;
  }

  @Override
  public String execute(String[] args) throws Exception {
    // ===============引用Debug文件夹内==========

    Debug_ping debugPing = new Debug_ping();
    Debug_PortMain debugPortMain=new Debug_PortMain();

    // =======================================
    if (args.length == 0) {
      return "🔧 调试工具\n\n" + getUsage();
    }
    String subCommand = args[0].toLowerCase();

    switch (subCommand) {
      case "ping":
        return debugPing.UsePing();
      case "port":
        return debugPortMain.getPortStatistics();
      default:
        return "❌ 不支持的调试命令: " + subCommand + "\n\n" + getUsage();
    }
  }

}