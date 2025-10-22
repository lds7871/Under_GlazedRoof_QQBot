package org.example.commands;

import org.springframework.stereotype.Component;

/**
 * 随机选择指令
 * 从多个选项中随机选择一个
 */
@Component
public class ChooseCommand extends BaseCommand {

  @Override
  public String getName() {
    return "choose";
  }

  @Override
  public String getDescription() {
    return "输入[选项1][选项2]或更多，随机选择";
  }

  @Override
  public String getUsage() {
    return "用法：/choose [选项1] [选项2] [选项3] ...\n" +
        "例如：/choose 苹果 香蕉 橙子";
  }

  @Override
  public String execute(String[] args) {
    String validation = validateMinArgs(args, 2);
    if (validation != null) {
      return validation;
    }

    String choice = args[(int) (Math.random() * args.length)];
    return "🤖 我选择：" + choice;
  }
}