package org.example.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 帮助指令
 * 显示所有可用指令的帮助信息
 */
@Component
public class HelpCommand extends BaseCommand {

  private final List<Command> allCommands;

  @Autowired
  public HelpCommand(List<Command> allCommands) {
    this.allCommands = allCommands;
  }

  @Override
  public String getName() {
    return "help";
  }

  @Override
  public String getDescription() {
    return "显示帮助信息";
  }

  @Override
  public String getUsage() {
    return "用法：\n" +
        "/help - 显示所有指令\n" +
        "/help [指令名] - 显示特定指令的详细信息";
  }

  @Override
  public String execute(String[] args) {
    if (args.length == 0) {
      return getAllCommandsHelp();
    } else {
      String commandName = args[0].toLowerCase();
      return getSpecificCommandHelp(commandName);
    }
  }

  private String getAllCommandsHelp() {
    StringBuilder sb = new StringBuilder();
    sb.append("🤖 QQ机器人指令帮助\n\n");

    for (Command command : allCommands) {
      // 跳过自己，避免循环依赖
      if (command == this)
        continue;

      sb.append("• /").append(command.getName())
          .append(" - ").append(command.getDescription())
          .append("\n");
    }

    sb.append("\n💡 使用 /help [指令名] 查看详细用法");
    sb.append("\n💡 在群聊中需要@机器人");

    return sb.toString();
  }

  private String getSpecificCommandHelp(String commandName) {
    for (Command command : allCommands) {
      if (command.getName().equalsIgnoreCase(commandName)) {
        return "📖 指令详情\n\n" +
            "指令：/" + command.getName() + "\n" +
            "描述：" + command.getDescription() + "\n\n" +
            command.getUsage();
      }
    }

    return "❌ 未找到指令：" + commandName + "\n" +
        "使用 /help 查看所有可用指令";
  }
}