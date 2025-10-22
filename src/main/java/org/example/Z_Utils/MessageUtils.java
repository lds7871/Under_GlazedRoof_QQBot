package org.example.Z_Utils;

import java.util.regex.Pattern;

/**
 * 消息处理工具类
 * 用于处理QQ消息的格式化、验证和清理
 */
public class MessageUtils {


    // 禁用的特殊字符模式
    private static final Pattern FORBIDDEN_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");


    /**
     * 为AI响应添加错误处理
     */
    public static String handleAiResponse(String response, String fallbackMessage) {

        if (response != null)
        {
            return response;
        }
        else
        {
            return fallbackMessage != null ? fallbackMessage : "🤖 抱歉，AI服务暂时无法响应，请稍后再试。";
        }
    }


    /**
     * 格式化错误消息
     */
    public static String formatErrorMessage(String originalCommand, Exception e) {
        if (originalCommand == "chat" || originalCommand == "joke") {
            return String.format("🚫 执行指令 /%s 时出现问题\n💡 接口请求超出配额", originalCommand);
        }
        return String.format("🚫 执行指令 /%s 时出现问题\n💡 请稍后重试或联系管理员", originalCommand);
    }
}