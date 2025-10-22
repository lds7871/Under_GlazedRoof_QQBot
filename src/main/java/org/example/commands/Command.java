package org.example.commands;

/**
 * 指令接口
 * 所有指令类都需要实现这个接口
 * 
 * @author QQ Robot Team
 * @since 2.0.0
 */
public interface Command {

  /**
   * 获取指令名称
   * 
   * @return 指令名称（不包含"/"前缀）
   */
  String getName();

  /**
   * 获取指令描述
   * 
   * @return 指令的简短描述
   */
  String getDescription();

  /**
   * 获取指令用法说明
   * 
   * @return 详细的用法说明
   */
  String getUsage();

  /**
   * 执行指令
   * 
   * @param args 指令参数
   * @return 执行结果
   */
  String execute(String[] args) throws Exception;
}