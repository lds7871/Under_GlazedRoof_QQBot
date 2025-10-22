package org.example.service;

import org.example.config.QQBotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * QQ机器人服务类
 * 处理与QQ机器人API的交互逻辑
 * 
 * @author QQ Robot Team
 * @since 1.0.0
 */
@Service
public class QQBotService {

    @Autowired
    private QQBotConfig config;

    @Autowired
    private CommandService commandService;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public QQBotService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理从Node.js服务转发过来的消息
     * 
     * @param eventType 事件类型
     * @param data      事件数据
     * @return 处理结果
     */
    public Map<String, Object> processMessageFromNode(String eventType, Map<String, Object> data) {
        System.out.println("[QQBotService] 处理Node.js转发的事件: " + eventType);

        if ("AT_MESSAGE_CREATE".equals(eventType)) {
            // 处理频道@消息
            String content = (String) data.get("content");
            String channelId = (String) data.get("channel_id");
            String guildId = (String) data.get("guild_id");
            String messageId = (String) data.get("id");

            System.out.println("[QQBotService] 频道消息内容: " + content);
            System.out.println("[QQBotService] 频道ID: " + channelId);
            System.out.println("[QQBotService] 频道GUILDID: " + guildId);

            // 尝试将消息作为指令处理，获取回复内容
            String replyContent = commandService.processCommand(content);
            
            if (replyContent != null && !replyContent.isEmpty()) {
                // 需要回复，让Node.js处理回复
                return Map.of(
                        "shouldReply", true,
                        "replyContent", replyContent,
                        "channelId", channelId,
                        "guildId", guildId,
                        "messageId", messageId,
                        "status", "COMMAND_HANDLED");
            } else {
                return Map.of(
                        "shouldReply", false,
                        "replyContent", "",
                        "status", "AT_MESSAGE_PROCESSED");
            }
        } else if ("GROUP_AT_MESSAGE_CREATE".equals(eventType)) {
            // 处理群聊@消息
            String content = (String) data.get("content");
            String groupOpenid = (String) data.get("group_openid");

            System.out.println("[QQBotService] 群聊消息内容: " + content);
            System.out.println("[QQBotService] 群聊ID: " + groupOpenid);

            // 尝试将消息作为指令处理
            String replyContent = commandService.processCommand(content);
            if (replyContent != null) {
                return Map.of(
                        "shouldReply", true,
                        "replyContent", replyContent,
                        "status", "COMMAND_HANDLED");
            } else {
                return Map.of(
                        "shouldReply", true,
                        "replyContent", "感谢您在群聊中@我，如果需要帮助，请发送 /help",
                        "status", "GROUP_AT_MESSAGE_PROCESSED");
            }
        } else if ("C2C_MESSAGE_CREATE".equals(eventType)) {
            // 处理私聊消息
            String content = (String) data.get("content");
            @SuppressWarnings("unchecked")
            Map<String, String> author = (Map<String, String>) data.get("author");
            String userOpenid = author.get("user_openid");

            System.out.println("[QQBotService] 私聊消息内容: " + content);
            System.out.println("[QQBotService] 用户ID: " + userOpenid);

            // 尝试将消息作为指令处理
            String replyContent = commandService.processCommand(content);
            if (replyContent != null) {
                return Map.of(
                        "shouldReply", true,
                        "replyContent", replyContent,
                        "status", "COMMAND_HANDLED");
            } else {
                return Map.of(
                        "shouldReply", true,
                        "replyContent", "您好！感谢您的私聊消息，如果需要帮助，请发送 /help",
                        "status", "C2C_MESSAGE_PROCESSED");
            }
        } else if ("DIRECT_MESSAGE_CREATE".equals(eventType)) {
            // 处理频道私聊消息
            String content = (String) data.get("content");
            String channelId = (String) data.get("channel_id");
            String guildId = (String) data.get("guild_id");

            System.out.println("[QQBotService] 频道私聊消息内容: " + content);
            System.out.println("[QQBotService] 频道私聊频道ID: " + channelId);
            System.out.println("[QQBotService] 频道ID: " + guildId);

            // 尝试将消息作为指令处理
            String replyContent = commandService.processCommand(content);
            if (replyContent != null) {
                return Map.of(
                        "shouldReply", true,
                        "replyContent", replyContent,
                        "status", "COMMAND_HANDLED");
            } else {
                return Map.of(
                        "shouldReply", true,
                        "replyContent", "您好！感谢您的频道私聊消息，如果需要帮助，请发送 /help",
                        "status", "DIRECT_MESSAGE_PROCESSED");
            }
        }

        return Map.of(
                "shouldReply", false,
                "replyContent", "",
                "status", "EVENT_IGNORED");
    }

    /**
     * 记录从Node.js服务转发过来的事件
     * 
     * @param eventType 事件类型
     * @param data      事件数据
     */
    public void logEvent(String eventType, Map<String, Object> data) {
        System.out.println("收到Node.js事件通知: " + eventType);
        // 在这里可以添加更详细的日志记录逻辑，例如写入文件或数据库
    }

    /**
     * 验证请求签名
     * 
     * @param headers 请求头
     * @param payload 请求体
     * @return 是否验证通过
     */
    public boolean verifySignature(Map<String, String> headers, String payload) {
        try {
            String timestamp = headers.get("x-signature-timestamp");
            String signature = headers.get("x-signature-ed25519");

            if (timestamp == null || signature == null) {
                return false;
            }

            // 构建待签名字符串
            String toSign = timestamp + payload;

            // 使用HMAC-SHA256计算签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    config.getSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            return calculatedSignature.equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 处理接收到的消息
     * 
     * @param payload 消息载荷
     * @return 处理结果
     */
    public String handleMessage(String payload) {
        try {
            Map<String, Object> message = objectMapper.readValue(payload, Map.class);

            // 获取消息类型
            String type = (String) message.get("t");
            Map<String, Object> data = (Map<String, Object>) message.get("d");

            switch (type) {
                case "READY":
                    handleReadyEvent(data);
                    break;
                case "AT_MESSAGE_CREATE":
                    handleAtMessageCreate(data);
                    break;
                case "MESSAGE_CREATE":
                    handleMessageCreate(data);
                    break;
                default:
                    System.out.println("Unknown message type: " + type);
                    break;
            }

            return "OK";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * 处理READY事件
     */
    private void handleReadyEvent(Map<String, Object> data) {
        System.out.println("机器人已连接，用户信息: " + data.get("user"));
    }

    /**
     * 处理@消息事件
     */
    private void handleAtMessageCreate(Map<String, Object> data) {
        System.out.println("收到@消息: " + data);

        String channelId = (String) data.get("channel_id");
        String content = (String) data.get("content");

        // 尝试将消息作为指令处理
        if (commandService.handleCommand(content, channelId)) {
            return; // 如果是指令，则不再执行后续逻辑
        }

        // 如果不是指令，执行原有的自动回复逻辑
        Map<String, Object> author = (Map<String, Object>) data.get("author");

        // 简单的自动回复逻辑
        if (content != null && content.contains("hello")) {
            sendMessage(channelId, "Hello! 我是QQ机器人，很高兴见到你！", "text");
        } else if (content != null && content.contains("时间")) {
            sendMessage(channelId, "当前时间: " + java.time.LocalDateTime.now(), "text");
        } else {
            sendMessage(channelId, "收到你的消息了！", "text");
        }
    }

    /**
     * 处理普通消息事件
     */
    private void handleMessageCreate(Map<String, Object> data) {
        System.out.println("收到消息: " + data);
        // 普通消息处理逻辑
    }

    /**
     * 发送消息
     * 
     * @param channelId 频道ID
     * @param content   消息内容
     * @param msgType   消息类型
     * @return 发送结果
     */
    public Map<String, Object> sendMessage(String channelId, String content, String msgType) {
        try {
            Map<String, Object> messageBody = new HashMap<>();
            messageBody.put("content", content);
            messageBody.put("msg_type", msgType);

            String url = config.getApiBaseUrl() + "/channels/" + channelId + "/messages";

            String response = webClient.post()
                    .uri(url)
                    .header("Authorization", "QQBot " + config.getToken())
                    .header("Content-Type", "application/json")
                    .bodyValue(messageBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return Map.of("success", true, "response", response);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 获取机器人信息
     */
    public Map<String, Object> getBotInfo() {
        try {
            String url = config.getApiBaseUrl() + "/users/@me";

            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bot " + config.getAppId() + "." + config.getToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readValue(response, Map.class);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取频道列表
     */
    public Map<String, Object> getGuilds() {
        try {
            String url = config.getApiBaseUrl() + "/users/@me/guilds";

            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bot " + config.getAppId() + "." + config.getToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readValue(response, Map.class);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取子频道列表
     */
    public Map<String, Object> getChannels(String guildId) {
        try {
            String url = config.getApiBaseUrl() + "/guilds/" + guildId + "/channels";

            String response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bot " + config.getAppId() + "." + config.getToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readValue(response, Map.class);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

}