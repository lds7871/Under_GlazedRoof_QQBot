package org.example.controller;

import org.example.service.QQBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * QQ机器人消息处理控制器
 * 处理QQ机器人的Webhook回调和API调用
 * 
 * @author QQ Robot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/qq")
@CrossOrigin(origins = "*")
public class QQBotController {
    
    @Autowired
    private QQBotService qqBotService;
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = Map.of(
            "status", "OK",
            "service", "QQ Robot API",
            "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * 接收QQ机器人的Webhook消息回调
     * 
     * @param headers 请求头信息
     * @param payload 消息载荷
     * @return 处理结果
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload) {
        
        try {
            // 验证请求签名
            if (!qqBotService.verifySignature(headers, payload)) {
                return ResponseEntity.status(401).body("Invalid signature");
            }
            
            // 处理消息
            String result = qqBotService.handleMessage(payload);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * 发送消息到QQ频道
     * 
     * @param request 发送消息请求
     * @return 发送结果
     */
    @PostMapping("/send-message")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            String channelId = (String) request.get("channelId");
            String content = (String) request.get("content");
            String msgType = (String) request.getOrDefault("msgType", "text");
            
            if (channelId == null || content == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required parameters: channelId, content"
                ));
            }
            
            Map<String, Object> result = qqBotService.sendMessage(channelId, content, msgType);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send message: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取机器人信息
     */
    @GetMapping("/bot-info")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        try {
            Map<String, Object> botInfo = qqBotService.getBotInfo();
            return ResponseEntity.ok(botInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get bot info: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取频道列表
     */
    @GetMapping("/guilds")
    public ResponseEntity<Map<String, Object>> getGuilds() {
        try {
            Map<String, Object> guilds = qqBotService.getGuilds();
            return ResponseEntity.ok(guilds);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get guilds: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取指定频道的子频道列表
     */
    @GetMapping("/guilds/{guildId}/channels")
    public ResponseEntity<Map<String, Object>> getChannels(@PathVariable String guildId) {
        try {
            Map<String, Object> channels = qqBotService.getChannels(guildId);
            return ResponseEntity.ok(channels);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get channels: " + e.getMessage()
            ));
        }
    }
}