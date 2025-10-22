package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * 指令处理服务
 * 解析和执行用户在群聊中发送的指令
 * 
 * @author QQ Robot Team
 * @since 1.1.0
 */
@Service
public class CommandService {

    private final QQBotService qqBotService;

    // 存储指令及其对应的处理器
    private final Map<String, Function<String[], String>> commandHandlers;

    /**
     * 构造函数，通过CommandRegistry初始化所有指令
     */
    @Autowired
    public CommandService(@Lazy QQBotService qqBotService, CommandRegistry commandRegistry) {
        this.qqBotService = qqBotService;
        this.commandHandlers = commandRegistry.getAllCommands();
    }

    /**
     * 处理指令并返回回复内容（用于Node.js服务）
     * 
     * @param content 消息内容
     * @return 如果是指令，返回回复内容；否则返回null
     */
    public String processCommand(String content) {
        System.out.println("[CommandService] 处理消息: " + content);

        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // 移除@机器人的前缀，只保留指令部分
        String cleanContent = content.trim();

        // 如果消息包含@，尝试提取@后面的内容
        if (cleanContent.contains("@")) {
            // 查找最后一个@符号后的内容
            int lastAtIndex = cleanContent.lastIndexOf("@");
            if (lastAtIndex != -1) {
                // 找到@符号后的下一个空格，提取指令部分
                String afterAt = cleanContent.substring(lastAtIndex);
                int spaceIndex = afterAt.indexOf(" ");
                if (spaceIndex != -1) {
                    cleanContent = afterAt.substring(spaceIndex + 1).trim();
                } else {
                    // 如果@后面没有空格，说明没有指令
                    return null;
                }
            }
        }

        System.out.println("[CommandService] 清理后的消息: " + cleanContent);

        // 指令必须以 "/" 开头
        if (!cleanContent.startsWith("/")) {
            return null;
        }

        // 解析指令和参数
        String[] parts = cleanContent.substring(1).split("\\s+");
        String command = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        System.out.println("[CommandService] 解析到指令: " + command + ", 参数: " + Arrays.toString(args));

        // 查找指令处理器
        Function<String[], String> handler = commandHandlers.get(command);

        if (handler != null) {
            try {
                // 执行指令并获取回复内容
                String reply = handler.apply(args);
                System.out.println("指令 '" + command + "' 已执行，回复: " + reply);
                return reply;
            } catch (Exception e) {
                e.printStackTrace();
                return "执行指令 '" + command + "' 时发生错误。";
            }
        } else {
            // 如果指令未找到，返回提示信息
            return "未知的指令: " + command + "\n发送 /help 查看所有可用指令。";
        }
    }

    /**
     * 处理收到的消息，检查是否为指令（保留原方法用于向后兼容）
     * 现在只返回回复文本，不直接调用API
     * 
     * @param content   消息内容
     * @param channelId 频道ID（已弃用，改为在调用方处理回复）
     * @return 如果是指令并被成功处理，返回true；否则返回false
     */
    @Deprecated
    public boolean handleCommand(String content, String channelId) {
        System.out.println("[CommandService] [DEPRECATED] 调用了handleCommand，应改用processCommand");
        // 直接调用新的processCommand方法，忽略channelId
        String reply = processCommand(content);
        return reply != null && !reply.isEmpty();
    }
}