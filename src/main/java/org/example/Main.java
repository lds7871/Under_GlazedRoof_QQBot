package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * QQ机器人SpringBoot主应用类
 * 
 * @author QQ Robot Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
public class Main {
    
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("QQ机器人服务启动成功！");
        System.out.println("访问 http://localhost:8070 查看服务状态");
        System.out.println("机器人消息回调地址: http://localhost:8070/qq/webhook");
    }
}