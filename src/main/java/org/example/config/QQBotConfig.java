package org.example.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * QQ机器人配置类
 * 
 * @author QQ Robot Team
 * @since 1.0.0
 */
@Getter
@Setter
@ToString(exclude = { "token", "secret", "verifyToken", "encryptKey" })
@Configuration
@ConfigurationProperties(prefix = "qq.bot")
public class QQBotConfig {

    /**
     * 机器人应用ID (BotAppID)
     */
    private String appId;

    /**
     * 机器人密钥 (BotToken)
     */
    private String token;

    /**
     * 机器人私钥 (用于签名验证)
     */
    private String secret;

    /**
     * 是否为沙箱环境
     */
    @Setter(AccessLevel.NONE)
    private boolean sandbox = true;

    /**
     * API基础URL
     */
    private String apiBaseUrl = "https://sandbox.api.sgroup.qq.com";

    /**
     * WebSocket网关URL
     */
    private String wsUrl = "wss://sandbox.api.sgroup.qq.com/websocket";

    /**
     * 消息回调验证Token
     */
    private String verifyToken;

    /**
     * 加密密钥
     */
    private String encryptKey;

    /**
     * 自定义setSandbox方法，用于根据环境自动切换API地址
     */
    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
        // 根据环境自动切换API地址
        if (sandbox) {
            this.apiBaseUrl = "https://sandbox.api.sgroup.qq.com";
            this.wsUrl = "wss://sandbox.api.sgroup.qq.com/websocket";
        } else {
            this.apiBaseUrl = "https://api.sgroup.qq.com";
            this.wsUrl = "wss://api.sgroup.qq.com/websocket";
        }
    }
}