package org.example.commands;

import org.springframework.stereotype.Component;

/**
 * Echo指令
 * 重复用户输入的内容
 */
@Component
public class EchoCommand extends BaseCommand {

  @Override
  public String getName() {
    return "echo";
  }

  @Override
  public String getDescription() {
    return "输入[内容]重复你说的话";
  }

  @Override
  public String getUsage() {
    return "用法：/echo [内容]\n例如：/echo 你好世界";
  }

  @Override
  public String execute(String[] args) {
    String validation = validateMinArgs(args, 1);
    if (validation != null) {
      return validation;
    }

    return joinArgs(args, 0);
  }
}