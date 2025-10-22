package org.example.controller;

import org.example.service.QQBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Node.js服务通信控制器
 * 处理来自Node.js机器人服务的请求
 * 
 * @author QQ Robot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/qq")
@CrossOrigin(origins = "*")
public class NodeCommunicationController {
    
    @Autowired
    private QQBotService qqBotService;
    
    /**
     * 处理来自Node.js的消息处理请求
     * 
     * @param request 消息处理请求
     * @return 处理结果，包括是否需要回复和回复内容
     */
    @PostMapping("/process-message")
    public ResponseEntity<Map<String, Object>> processMessage(@RequestBody Map<String, Object> request) {
        try {
            String eventType = (String) request.get("eventType");
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            // 根据事件类型处理消息
            Map<String, Object> result = qqBotService.processMessageFromNode(eventType, data);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "处理消息失败: " + e.getMessage(),
                "shouldReply", false
            ));
        }
    }
    
    /**
     * 接收来自Node.js的事件通知
     * 
     * @param request 事件通知请求
     * @return 处理结果
     */
    @PostMapping("/event-notification")
    public ResponseEntity<String> eventNotification(@RequestBody Map<String, Object> request) {
        try {
            String eventType = (String) request.get("eventType");
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            // 记录事件日志
            qqBotService.logEvent(eventType, data);
            
            return ResponseEntity.ok("Event received");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Event processing failed");
        }
    }
}