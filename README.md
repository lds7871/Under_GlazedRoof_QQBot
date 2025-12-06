# 🤖 QQ 机器人项目 (Under_GlazedRoof_QQBot)
<div align="center">

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17+-brightgreen.svg)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.1.4-brightgreen.svg)
![Node.js](https://img.shields.io/badge/Node.js-16+-brightgreen.svg)
![Status](https://img.shields.io/badge/Status-Active-success.svg)

一个功能完整的 QQ 机器人解决方案，基于 **SpringBoot** + **Node.js** + **Websocket** 的 QQ 官方 Bot API v2 实现。

</div>

## 前言：
- **本人还有另一个基于NapCat框架开发的QQBot，功能更加自由[GlazedTile_QQBot](https://github.com/lds7871/GlazedTile_QQBot)。**
- **此项目发出来估计就不再会更新了，是从私有库改过来的已经更新不少次了，QQ官机和频道功能两年里一直在砍，QQ开放平台文档本来就烂还两年多不更新，哪些弃用了也不明确。越写越伤心**
- **即使如此你想快速开发仅有文字返回的指令或是熟悉技术，这个项目还是可用。**
- **文字指令开发指令请参考[QQ Bot 指令快速开发指南](3分钟快速开发指令.md)，Websocket服务器和Spring服务器都有比较详细的Debug日志输出，方便排查问题。**
- **对于文字返回的指令，推荐使用纯文本消息（msg_type:0），这样可以在私聊和群聊都能正常使用。**
- **开发新的接口只需要继承抽象类并完善你的逻辑即可。commands有足够的案例指令。**
- **但只要是任何别的返回格式（Markdown/Ark/Media），QQ都会有及其严格的使用限制和极慢的审核速度等着你。**

---
---

## ✨ 项目特性

### 🏗️ 架构设计
- **混合架构** - Spring Boot 处理业务逻辑，Node.js 管理 WebSocket 连接
- **模块化设计** - 清晰的代码结构，易于维护和扩展
- **解耦通信** - HTTP 通信实现服务间的完全解耦

### 🌐 通信支持
- **频道消息** - 支持 `AT_MESSAGE_CREATE` 频道内 @ 消息
- **群聊消息** - 支持 `GROUP_AT_MESSAGE_CREATE` 群聊 @ 消息
- **私聊消息** - 支持 `C2C_MESSAGE_CREATE` 和 `DIRECT_MESSAGE_CREATE` 私聊
- **实时处理** - WebSocket 长连接实时接收消息

### 🤖 指令系统
- **模板内置指令** - `/debug`, `/echo`, `/help` 等
- **灵活的扩展机制** - 轻松添加自定义指令
- **自动指令发现** - 基于注解的指令注册系统
- **完整的指令处理流程** - 从消息解析到回复发送

### 🛡️ 安全可靠
- **沙箱环境支持** - 内置沙箱模式，安全测试机器人功能
- **自动 Token 刷新** - 无需手动管理 AccessToken
- **心跳维护** - 自动保持 WebSocket 长连接
- **异常捕获处理** - 完善的错误处理机制

### 🔧 开发友好
- **无第三方 SDK** - 直接调用官方 API，代码透明
- **详细日志** - 彩色表情符号日志，易于调试
- **本地开发工具** - 内置启动脚本，开箱即用

---


## 📋 目录
1. [现有指令](#1-现有指令)
2. [服务配置与快速启动](#2-服务配置与快速启动)
3. [目录架构](#3-目录架构)
4. [服务响应流程](#4-服务响应流程)
5. [常见问题](#5-常见问题)
6. [技术栈](#6-技术栈)

---


## 1. 现有指令

| 指令 | 类 | 功能 | 示例 |
|------|-----|------|------|
| `/help` | HelpCommand | 显示所有可用指令或特定指令的用法 | `/help` 或 `/help echo` |
| `/echo` | EchoCommand | 复读用户输入的内容 | `/echo 你好` → `你好` |
| `/choose` | ChooseCommand | 从多个选项中随机选择一个 | `/choose 选项1 选项2 选项3` |
| `/debug` | DebugCommand | 调试工具（支持子命令：ping、port 等） | `/debug [ping]/[port]` |
| `/img` | ImageExampleCommand | 发送图片消息（Markdown 模板格式） | `MarkDown测试案例` |
| `/++` | 其实还写过不少接口 | 包括AI聊天/API接口调用/模拟浏览器获取信息 | `指令已删除，Pom依赖还留着` |

### 发送方式

```
私聊：直接输入 /指令名 参数
群聊：@机器人 /指令名 参数
频道：在频道中 @机器人 /指令名 参数
频道私聊：在频道中私聊 @机器人 /指令名 参数
```

---

## 2. 服务配置与快速启动

### 前置要求

- **Java 17+**
- **Node.js 16+**
- **Maven 3.8+**
- **QQ Bot 应用凭证**（在 [QQ 开放平台](https://bot.q.qq.com) 申请,获取你的环境配置信息）

### 环境配置

#### 2.1 配置 `.env` 文件

在项目根目录创建 `.env` 文件：

```env
# QQ Bot API 凭证
QQ_BOT_APP_ID=你的APP_ID
QQ_BOT_SECRET=你的SECRET
QQ_BOT_SANDBOX=true

# SpringBoot 服务
SPRING_BOOT_URL=http://localhost:8070
```

#### 2.2 配置 `application.yml`

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8070

qq:
  bot:
    app-id: ${QQ_BOT_APP_ID}
    secret: ${QQ_BOT_SECRET}
    sandbox: ${QQ_BOT_SANDBOX:true}
```

### 快速启动

#### 方案 1：分别启动（推荐开发时使用）

```bash
# 终端 1：启动 Java 服务
mvn spring-boot:run

# 终端 2：启动 Node.js 服务
node nodejs/qq-bot-service.js
```

#### 方案 2：一键启动

```bash
# 同时启动两个服务
npm start

# 或使用开发模式（支持热更新）
npm run dev
```

#### 方案 3：编译后启动

```bash
# 编译项目
mvn clean package

# 启动 Jar 包
java -jar target/QQR-1.0-SNAPSHOT.jar

# 启动 Node.js
node nodejs/qq-bot-service.js
```

### 验证启动成功

```bash
# 查看 Java 服务状态
curl -X GET http://localhost:8070/qq/status

# 查看日志
tail -f logs/application.log
```

---

## 3. 目录架构

```
QQR/
├── src/main/java/org/example/
│   ├── commands/              # 指令处理模块
│   │   ├── Command.java       # 指令接口
│   │   ├── BaseCommand.java   # 抽象基类（提供工具方法）
│   │   ├── HelpCommand.java   # /help
│   │   ├── EchoCommand.java   # /echo
│   │   ├── ChooseCommand.java # /choose
│   │   ├── DebugCommand.java  # /debug
│   │   ├── ImageExampleCommand.java  # /img
│   │   └── TemplateMarkdownCommand.java  # 模板 Markdown 基类
│   ├── service/               # 业务逻辑层
│   │   ├── CommandService.java      # 指令处理与分发
│   │   ├── CommandRegistry.java     # 指令自动注册
│   │   └── QQBotService.java        # 消息处理与回复
│   ├── controller/            # 控制层
│   │   ├── QQBotController.java     # Node.js 消息接收端点
│   │   └── NodeCommunicationController.java
│   ├── config/                # 配置类
│   │   └── QQBotConfig.java   # 机器人配置
│   └── Main.java              # 启动类
├── nodejs/
│   └── qq-bot-service.js      # Node.js WebSocket 服务
├── .env                       # 环境变量配置
├── application.yml            # SpringBoot 配置
├── pom.xml                    # Maven 依赖
└── package.json               # Node.js 依赖
```

---

## 4. 服务响应流程

### 消息处理流程图

```
┌─────────────────────┐
│  QQ 用户发送消息     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Node.js WebSocket   │ ◄─── 接收 WebSocket 消息
│  (qq-bot-service.js)│
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  转发至 SpringBoot  │ ◄─── POST /qq/process-message
│  (8070 端口)        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  QQBotService       │ ◄─── 判断消息类型
│  processMessage()   │
└──────────┬──────────┘
           │
      ┌────┴─────┐
      │           │
      ▼           ▼
   ┌──────┐   ┌────────┐
   │指令？│   │普通消息│
   └───┬──┘   └────┬───┘
       │            │
       ▼            ▼
┌──────────────┐ ┌──────────┐
│CommandService│ │回复模板  │
│procesCommand │ │消息      │
└───┬──────────┘ └────┬─────┘
    │                 │
    ▼                 ▼
┌──────────────────────────┐
│ 返回 JSON 格式回复内容   │
└───┬──────────────────────┘
    │
    ▼
┌──────────────────────┐
│ Node.js 处理回复     │
│ - msg_type 检测      │
│ - 主动/回复 消息判断 │
└───┬───────────────────┘
    │
    ▼
┌──────────────────────┐
│ 调用 QQ Bot API      │
│ 发送消息至用户       │
└──────────────────────┘
```

### 指令执行流程

```
1️⃣ 命令解析
   /echo hello world
   ├─ 指令: echo
   └─ 参数: ["hello", "world"]

2️⃣ 指令查找
   CommandRegistry → allCommands Map
   查找 "echo" → EchoCommand 实例

3️⃣ 指令执行
   EchoCommand.execute(["hello", "world"])
   返回: "hello world"

4️⃣ 响应封装
   {
     "msg_type": 0,
     "content": "hello world",
     "msg_id": "..."
   }

5️⃣ 消息发送
   Node.js POST 至 QQ Bot API
```

### 消息类型处理

| msg_type | 类型 | 是否需要 msg_id | 用途 |
|----------|------|----------------|------|
| 0 | 纯文本 | ✅ 识别到URL直接拦截 | 普通文本回复 |
| 2 | Markdown | ❌ 日活没2000不配用 | 模板/富文本回复 |
| 3 | Ark | ❌ 日活没2000不配用 | 卡片消息 |
| 7 | Media | ❌ 日活没2000不配用 | 图片/视频消息 |

---

## 5. 常见问题

### Q1: 启动后 Node.js 报错 "reply message not allow markdown"？

**原因**：私聊消息中，带 `msg_id` 的回复消息不能使用 Markdown 格式。

**解决**：
- 特殊消息（Markdown/Ark/Media）作为**主动消息**发送（移除 `msg_id`）
- 纯文本消息作为**回复消息**发送（保留 `msg_id`）

### Q2: 私聊中指令没有响应？

**排查步骤**：
1. 检查 `.env` 中 `QQ_BOT_APP_ID` 和 `QQ_BOT_SECRET` 是否正确
2. 查看 Node.js 日志是否显示消息已接收
3. 检查 SpringBoot 日志中指令是否被正确解析
4. 确认指令使用 `/` 开头，格式正确

### Q3: 如何新增自定义指令？

**步骤**：
1. 在 `src/main/java/org/example/commands/` 创建新类（例如 `MyCommand.java`）
2. 继承 `BaseCommand` 或 `TemplateMarkdownCommand`
3. 实现必要方法：`getName()`、`getDescription()`、`execute(String[] args)`
4. 添加 `@Component` 注解让 Spring 自动注册
5. 编译并重启服务

### Q4: 如何发送图片消息？

**方式**：
```bash
# 使用默认示例图片
/img

# 使用自定义 URL
/img https://example.com/image.png
```

**技术细节**：
- 使用 Markdown 模板格式（`msg_type: 2`）
- 模板 ID：`102813362_1760679605`（官方"主机返回"模板）
- 参数：只需 `ReturnImg`（图片 URL）

### Q5: 群聊和私聊的差异？

| 特性 | 群聊 | 私聊 |
|------|------|------|
| 触发方式 | @机器人 /指令 | 直接 /指令 |
| 消息格式 | 回复消息（需 msg_id） | 主动消息（特殊格式无 msg_id） |
| Markdown 支持 | ✅ 回复支持 | ✅ 主动消息支持 |
| 指令响应 | 公开回复 | 私密回复 |

---

## 6. 技术栈

### 后端 (Java)

| 组件 | 版本 | 用途 |
|-----|------|------|
| **Spring Boot** | 3.1.4 | 应用框架 |
| **Java** | 17 | 编程语言 |
| **Maven** | 3.8+ | 依赖管理 |
| **Jackson** | - | JSON 序列化 |

### 前端通信 (Node.js)

| 组件 | 版本 | 用途 |
|-----|------|------|
| **Node.js** | 16+ | 运行时 |
| **Axios** | ^1.6.0 | HTTP 客户端 |
| **WebSocket** | ^8.18.3 | WebSocket 连接 |
| **dotenv** | ^16.6.1 | 环境变量管理 |

### QQ Bot API

- **API 版本**：v2（官方最新版本）
- **认证方式**：OAuth 2.0（Access Token）
- **传输协议**：WebSocket（消息接收）+ HTTP（消息发送）
- **沙箱环境**：支持开发测试

### 开发工具

```bash
# 编译和构建
mvn clean compile   # 编译
mvn clean package   # 打包
mvn clean test      # 运行测试

# Node.js
npm install         # 安装依赖
npm start          # 启动服务
npm run dev        # 开发模式（热更新）
```

---

## 🚀 快速命令参考

```bash
# 初始化项目
npm run setup

# 开发模式启动（推荐）
npm run dev

# 生产模式启动
npm start

# 仅启动 Java
mvn spring-boot:run

# 仅启动 Node.js
node nodejs/qq-bot-service.js

# 构建 Jar 包
mvn clean package

# 查看帮助
/help
```

---

## 📞 技术与架构支持

- **QQ Bot 官方文档**: https://bot.q.qq.com/wiki/
- **QQ 开放平台**: https://open.qq.com/
- **Spring Boot**: https://spring.io/projects/spring-boot

---

- **最后更新**: 2025-10-22  
- **维护者**:
-   <a href="https://github.com/lds7871">
        <img src="https://github.com/lds7871.png?size=20" width="20" height="20" style="border-radius:50%;">
    </a>
- **谨以此纪念我胎死腹中的bot**:
- <img src="./胎死腹中的Bot.png" alt="bot" width="300"  />
