package org.example.service;

import org.example.commands.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 指令注册器
 * 自动发现和注册所有指令类
 * 
 * @author QQ Robot Team
 * @since 2.0.0
 */
@Component
public class CommandRegistry {

  private final List<Command> commands;

  /**
   * 构造函数，Spring会自动注入所有实现了Command接口的类
   */
  @Autowired
  public CommandRegistry(List<Command> commands) {
    this.commands = commands;
  }

  /**
   * 获取所有注册的指令处理器
   * 
   * @return 指令名称到处理函数的映射
   */

  public Map<String, Function<String[], String>> getAllCommands() {
    Map<String, Function<String[], String>> commandMap = new HashMap<>();

    for (Command command : commands) {
      commandMap.put(command.getName(), args -> {
        try {
          return command.execute(args);
        } catch (Exception e) {
          throw new RuntimeException("执行指令失败: " + command.getName(), e);
        }
      });
    }

    return commandMap;
  }
}