const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

console.log('dotenv配置路径:', path.join(__dirname, '..', '.env'));
console.log('当前工作目录:', process.cwd());
const axios = require('axios');
const WebSocket = require('ws');

// QQ机器人Node.js服务 - 使用直接API调用替代废弃的SDK
// 基于官方API文档: https://bot.q.qq.com/wiki/develop/api-v2/

class QQBotDirectService {
  constructor() {
    // SpringBoot API配置
    this.springBootUrl = process.env.SPRING_BOOT_URL || 'http://localhost:8070';

    // 机器人配置
    console.log('加载环境变量:');
    console.log('QQ_BOT_APP_ID:', process.env.QQ_BOT_APP_ID);
    console.log('QQ_BOT_SECRET:', process.env.QQ_BOT_SECRET ? '已设置' : '未设置');
    console.log('QQ_BOT_SANDBOX:', process.env.QQ_BOT_SANDBOX);

    const sandboxEnv = process.env.QQ_BOT_SANDBOX;
    const isSandbox = sandboxEnv === undefined ? true : (sandboxEnv.trim().toLowerCase() === 'true');

    this.botConfig = {
      appID: process.env.QQ_BOT_APP_ID,
      clientSecret: process.env.QQ_BOT_SECRET,
      sandbox: isSandbox
    };

    // API配置
    this.apiBaseUrl = this.botConfig.sandbox
      ? 'https://sandbox.api.sgroup.qq.com'
      : 'https://api.sgroup.qq.com';

    this.wsGatewayUrl = this.botConfig.sandbox
      ? 'wss://sandbox.api.sgroup.qq.com/websocket'
      : 'wss://api.sgroup.qq.com/websocket';

    this.accessToken = null;
    this.tokenExpiry = null;
    this.ws = null;
    this.sessionId = null;
    this.heartbeatInterval = null;
    this.lastHeartbeatAck = true;

    console.log('机器人配置:');
    console.log('- AppID:', this.botConfig.appID);
    console.log('- 沙箱模式:', this.botConfig.sandbox);
    console.log('- API地址:', this.apiBaseUrl);
    console.log('- WebSocket地址:', this.wsGatewayUrl);
  }

  // 获取AccessToken
  async getAccessToken() {
    try {
      // 检查是否有有效的AccessToken
      if (this.accessToken && this.tokenExpiry && Date.now() < this.tokenExpiry - 60000) {
        return this.accessToken;
      }

      console.log('正在获取AccessToken...');

      const response = await axios.post('https://bots.qq.com/app/getAppAccessToken', {
        appId: this.botConfig.appID,
        clientSecret: this.botConfig.clientSecret
      }, {
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (response.data && response.data.access_token) {
        this.accessToken = response.data.access_token;
        this.tokenExpiry = Date.now() + (response.data.expires_in * 1000);
        console.log('✅ AccessToken获取成功，有效期:', new Date(this.tokenExpiry).toLocaleString());
        return this.accessToken;
      } else {
        throw new Error('获取AccessToken失败: ' + JSON.stringify(response.data));
      }
    } catch (error) {
      console.error('获取AccessToken出错:', error.response?.data || error.message);
      throw error;
    }
  }

  // 创建API请求头
  async createHeaders() {
    const token = await this.getAccessToken();
    return {
      'Authorization': `QQBot ${token}`,
      'Content-Type': 'application/json',
      'User-Agent': 'QQ-Bot-Direct-Service/1.0'
    };
  }

  // 获取WebSocket网关信息
  async getGateway() {
    try {
      console.log('正在获取WebSocket网关信息...');
      const headers = await this.createHeaders();

      const response = await axios.get(`${this.apiBaseUrl}/gateway/bot`, {
        headers
      });

      console.log('✅ 网关信息获取成功:', response.data);
      return response.data;
    } catch (error) {
      console.error('获取网关信息失败:', error.response?.data || error.message);
      throw error;
    }
  }

  // 获取机器人信息
  async getBotInfo() {
    try {
      console.log('正在获取机器人信息...');
      const headers = await this.createHeaders();

      const response = await axios.get(`${this.apiBaseUrl}/users/@me`, {
        headers
      });

      console.log('✅ 机器人信息:', response.data);
      return response.data;
    } catch (error) {
      console.error('获取机器人信息失败:', error.response?.data || error.message);
      throw error;
    }
  }

  // 连接WebSocket
  async connectWebSocket() {
    try {
      // 获取网关信息
      const gateway = await this.getGateway();
      let wsUrl = gateway.url;

      // 如果网关返回的URL不是沙箱地址，但我们在沙箱环境，需要替换
      if (this.botConfig.sandbox && !wsUrl.includes('sandbox')) {
        wsUrl = wsUrl.replace('wss://api.sgroup.qq.com', 'wss://sandbox.api.sgroup.qq.com');
        console.log('沙箱模式，使用沙箱WebSocket地址:', wsUrl);
      }

      console.log('正在连接WebSocket:', wsUrl);

      this.ws = new WebSocket(wsUrl);

      this.ws.on('open', () => {
        console.log('✅ WebSocket连接已建立');
        console.log('当前环境：', this.botConfig.sandbox ? '🔒 沙箱环境' : '🌐 正式环境');
        this.sendIdentify();
      });

      this.ws.on('message', (data) => {
        this.handleWebSocketMessage(data);
      });

      this.ws.on('close', (code, reason) => {
        console.log(`WebSocket连接关闭: ${code} - ${reason}`);
        this.cleanup();
        // 自动重连
        setTimeout(() => {
          console.log('尝试重新连接...');
          this.connectWebSocket();
        }, 5000);
      });

      this.ws.on('error', (error) => {
        console.error('WebSocket连接错误:', error);
      });

    } catch (error) {
      console.error('连接WebSocket失败:', error);
      throw error;
    }
  }

  // 发送身份验证
  async sendIdentify() {
    try {
      const token = await this.getAccessToken();
      const identifyPayload = {
        op: 2, // IDENTIFY
        d: {
          token: `QQBot ${token}`,
          intents: (1 << 30) | (1 << 25) | (1 << 12), // PUBLIC_GUILD_MESSAGES(AT_MESSAGE_CREATE) | GROUP_AND_C2C_EVENT | DIRECT_MESSAGES(频道私聊)
          shard: [0, 1],
          properties: {
            $os: 'linux',
            $browser: 'qq-bot-direct-service',
            $device: 'qq-bot-direct-service'
          }
        }
      };
      console.log('发送身份验证...');
      const intentsValue = (1 << 30) | (1 << 25) | (1 << 12);
      console.log('intents 值:', intentsValue, '(PUBLIC_GUILD_MESSAGES | GROUP_AND_C2C_EVENT | DIRECT_MESSAGES)');
      this.ws.send(JSON.stringify(identifyPayload));
    } catch (error) {
      console.error('发送身份验证失败:', error);
    }
  }

  // 发送心跳
  sendHeartbeat() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const heartbeatPayload = {
        op: 1, // HEARTBEAT
        d: null
      };

      if (!this.lastHeartbeatAck) {
        console.log('⚠️ 上次心跳未收到ACK，可能连接有问题');
      }

      this.lastHeartbeatAck = false;
      this.ws.send(JSON.stringify(heartbeatPayload));
      console.log('💓 发送心跳');
    }
  }

  // 处理WebSocket消息
  handleWebSocketMessage(data) {
    try {
      const message = JSON.parse(data);
      console.log('📥 收到WebSocket消息 - op:', message.op, ', t:', message.t);

      switch (message.op) {
        case 0: // DISPATCH
          console.log('📤 分发事件:', message.t);
          this.handleDispatchEvent(message);
          break;
        case 1: // HEARTBEAT
          console.log('收到心跳请求');
          this.sendHeartbeat();
          break;
        case 7: // RECONNECT
          console.log('收到重连请求');
          this.ws.close();
          break;
        case 9: // INVALID_SESSION
          console.log('会话无效，重新连接...');
          this.sessionId = null;
          this.ws.close();
          break;
        case 10: // HELLO
          console.log('收到HELLO消息');
          if (message.d && message.d.heartbeat_interval) {
            this.startHeartbeat(message.d.heartbeat_interval);
          }
          break;
        case 11: // HEARTBEAT_ACK
          console.log('💚 收到心跳ACK');
          this.lastHeartbeatAck = true;
          break;
        default:
          console.log('❓ 未知消息类型:', message.op);
      }
    } catch (error) {
      console.error('处理WebSocket消息失败:', error);
    }
  }

  // 处理分发事件
  async handleDispatchEvent(message) {
    const eventType = message.t;
    const data = message.d;

    console.log(`\n📨 处理事件 [${eventType}]`);
    
    // 详细日志
    if (eventType === 'AT_MESSAGE_CREATE') {
      console.log('🎯 AT_MESSAGE_CREATE 详细信息:');
      console.log('  - content:', data.content);
      console.log('  - channel_id:', data.channel_id);
      console.log('  - guild_id:', data.guild_id);
      console.log('  - author.id:', data.author?.id);
      console.log('  - author.username:', data.author?.username);
      console.log('  - message.id:', data.id);
    }

    switch (eventType) {
      case 'READY':
        console.log('=== 🎉 机器人已就绪 ===');
        console.log('机器人信息:', data.user);
        console.log('Session ID:', data.session_id);
        this.sessionId = data.session_id;
        this.notifySpringBoot('READY', data);
        break;

      case 'AT_MESSAGE_CREATE':
        console.log('=== 📢 收到频道@消息 ===');
        console.log('消息内容:', data.content);
        console.log('频道ID:', data.channel_id);
        console.log('发送者:', data.author?.username);
        console.log('完整消息对象:', JSON.stringify(data));
        await this.handleAtMessage(data);
        break;

      case 'GROUP_AT_MESSAGE_CREATE':
        console.log('=== 👥 收到群聊@消息 ===');
        console.log('消息内容:', data.content);
        console.log('群聊ID:', data.group_openid);
        console.log('发送者ID:', data.author?.member_openid);
        await this.handleGroupAtMessage(data);
        break;

      case 'C2C_MESSAGE_CREATE':
        console.log('=== 💬 收到私聊消息 ===');
        console.log('消息内容:', data.content);
        console.log('用户ID:', data.author?.user_openid);
        await this.handleC2CMessage(data);
        break;

      case 'DIRECT_MESSAGE_CREATE':
        console.log('=== 🔐 收到频道私聊消息 ===');
        console.log('消息内容:', data.content);
        console.log('频道ID:', data.guild_id);
        console.log('私聊频道ID:', data.channel_id);
        console.log('发送者:', data.author?.username);
        await this.handleDirectMessage(data);
        break;

      case 'GUILD_CREATE':
        console.log('➕ 加入频道:', data.name, '(ID:', data.id, ')');
        break;

      case 'GUILD_DELETE':
        console.log('➖ 退出频道:', data.name);
        break;

      default:
        console.log(`⚠️  未处理的事件类型: ${eventType}`);
        console.log('原始数据:', JSON.stringify(data).substring(0, 500));
    }
  }

  // 处理@消息
  async handleAtMessage(messageData) {
    try {
      console.log('🔄 正在处理频道@消息...');

      // 转发消息到SpringBoot处理
      const response = await this.forwardToSpringBoot('AT_MESSAGE_CREATE', messageData);
      console.log('📩 SpringBoot响应:', response);

      // 根据SpringBoot的响应决定是否回复
      if (response && response.shouldReply && response.replyContent) {
        console.log('✉️  正在发送回复消息...');
        console.log('  内容:', response.replyContent);
        console.log('  频道ID:', response.channelId || messageData.channel_id);
        console.log('  原消息ID:', messageData.id);
        
        await this.replyMessage(messageData, response.replyContent);
      } else {
        console.log('ℹ️  无需回复');
      }
    } catch (error) {
      console.error('❌ 处理频道@消息失败:', error);
    }
  }

  // 处理群聊@消息
  async handleGroupAtMessage(messageData) {
    try {
      console.log('正在处理群聊@消息...');

      // 转发消息到SpringBoot处理
      const response = await this.forwardToSpringBoot('GROUP_AT_MESSAGE_CREATE', messageData);
      console.log('SpringBoot响应:', response);

      // 根据SpringBoot的响应决定是否回复
      if (response && response.shouldReply && response.replyContent) {
        console.log('正在发送群聊回复消息:', response.replyContent);
        await this.replyGroupMessage(messageData, response.replyContent);
      }
    } catch (error) {
      console.error('处理群聊@消息失败:', error);
    }
  }

  // 处理私聊消息
  async handleC2CMessage(messageData) {
    try {
      console.log('正在处理私聊消息...');

      // 转发消息到SpringBoot处理
      const response = await this.forwardToSpringBoot('C2C_MESSAGE_CREATE', messageData);
      console.log('SpringBoot响应:', response);

      // 根据SpringBoot的响应决定是否回复
      if (response && response.shouldReply && response.replyContent) {
        console.log('正在发送私聊回复消息:', response.replyContent);
        await this.replyC2CMessage(messageData, response.replyContent);
      }
    } catch (error) {
      console.error('处理私聊消息失败:', error);
    }
  }

  // 处理频道私聊消息（DIRECT_MESSAGE_CREATE）
  async handleDirectMessage(messageData) {
    try {
      console.log('正在处理频道私聊消息...');

      // 转发消息到SpringBoot处理
      const response = await this.forwardToSpringBoot('DIRECT_MESSAGE_CREATE', messageData);
      console.log('SpringBoot响应:', response);

      // 根据SpringBoot的响应决定是否回复
      if (response && response.shouldReply && response.replyContent) {
        console.log('正在发送频道私聊回复消息:', response.replyContent);
        await this.replyDirectMessage(messageData, response.replyContent);
      }
    } catch (error) {
      console.error('处理频道私聊消息失败:', error);
    }
  }
  async replyMessage(originalMessage, content) {
    try {
      console.log('发送频道回复消息...');
      const headers = await this.createHeaders();

      // 解析内容为JSON对象以检测是否为特殊消息类型
      let messageData = {};
      let isSpecialMessage = false;
      
      try {
        const parsedContent = JSON.parse(content);
        
        // 检查是否为 Markdown 消息（msg_type: 2）
        if (parsedContent.msg_type === 2 && parsedContent.markdown) {
          isSpecialMessage = true;
          // Markdown 消息不需要 content 字段和 msg_id
          messageData = {
            msg_type: 2,
            markdown: parsedContent.markdown
          };
        }
        // 检查是否为 Ark 消息（msg_type: 3）
        else if (parsedContent.msg_type === 3 && parsedContent.ark) {
          isSpecialMessage = true;
          // Ark 消息只需要 msg_type 和 ark 字段
          messageData = {
            msg_type: 3,
            ark: parsedContent.ark
          };
        }
        // 检查是否为 Media 富媒体消息（msg_type: 7）
        else if (parsedContent.msg_type === 7 && parsedContent.media) {
          isSpecialMessage = true;
          // Media 消息不需要 content 字段和 msg_id
          messageData = {
            msg_type: 7,
            media: parsedContent.media
          };
        }
        // 常规文本消息或其他格式
        else {
          messageData = {
            content: content,
            msg_type: 0,
            msg_id: originalMessage.id
          };
        }
      } catch (e) {
        // 如果不是JSON，作为普通文本处理
        messageData = {
          content: content,
          msg_type: 0,
          msg_id: originalMessage.id
        };
      }

      console.log('消息类型:', isSpecialMessage ? 'Markdown/Ark/Media' : '文本');
      console.log('消息数据:', JSON.stringify(messageData));

      const response = await axios.post(`${this.apiBaseUrl}/channels/${originalMessage.channel_id}/messages`, messageData, {
        headers
      });

      console.log('✅ 频道消息发送成功:', response.data);
      return response.data;
    } catch (error) {
      console.error('发送频道消息失败:', error.response?.data || error.message);
      throw error;
    }
  }

  // 回复群聊消息
  async replyGroupMessage(originalMessage, content) {
    try {
      console.log('发送群聊回复消息...');
      const headers = await this.createHeaders();

      // 解析内容为JSON对象以检测是否为特殊消息类型
      let messageData = {};
      let isSpecialMessage = false;
      
      try {
        const parsedContent = JSON.parse(content);
        
        // 检查是否为 Markdown 消息（msg_type: 2）
        if (parsedContent.msg_type === 2 && parsedContent.markdown) {
          isSpecialMessage = true;
          // Markdown 消息不需要 content 字段和 msg_id
          messageData = {
            msg_type: 2,
            markdown: parsedContent.markdown
          };
        }
        // 检查是否为 Ark 消息（msg_type: 3）
        else if (parsedContent.msg_type === 3 && parsedContent.ark) {
          isSpecialMessage = true;
          // Ark 消息只需要 msg_type 和 ark 字段
          messageData = {
            msg_type: 3,
            ark: parsedContent.ark
          };
        }
        // 检查是否为 Media 富媒体消息（msg_type: 7，包括图片）
        else if (parsedContent.msg_type === 7 && parsedContent.media) {
          isSpecialMessage = true;
          // Media 消息不需要 content 字段和 msg_id
          messageData = {
            msg_type: 7,
            media: parsedContent.media
          };
        }
        // 常规文本消息或其他格式
        else {
          messageData = {
            content: content,
            msg_type: 0,
            msg_id: originalMessage.id
          };
        }
      } catch (e) {
        // 如果不是JSON，作为普通文本处理
        messageData = {
          content: content,
          msg_type: 0,
          msg_id: originalMessage.id
        };
      }

      console.log('消息类型:', isSpecialMessage ? 'Markdown/Ark/Media' : '文本');
      console.log('消息数据:', JSON.stringify(messageData));

      const response = await axios.post(`${this.apiBaseUrl}/v2/groups/${originalMessage.group_openid}/messages`, messageData, {
        headers
      });

      console.log('✅ 群聊消息发送成功:', response.data);
      return response.data;
    } catch (error) {
      console.error('发送群聊消息失败:', error.response?.data || error.message);
      throw error;
    }
  }

  // 回复私聊消息
  async replyC2CMessage(originalMessage, content) {
    try {
      console.log('发送私聊回复消息...');
      const headers = await this.createHeaders();

      // 解析内容为JSON对象以检测是否为特殊消息类型
      let messageData = {};
      let isSpecialMessage = false;
      
      try {
        const parsedContent = JSON.parse(content);
        
        // 检查是否为 Markdown 消息（msg_type: 2）
        if (parsedContent.msg_type === 2 && parsedContent.markdown) {
          isSpecialMessage = true;
          // 私聊 Markdown 消息需要 msg_id，才能作为回复消息而不是主动消息
          messageData = {
            msg_type: 2,
            markdown: parsedContent.markdown,
            msg_id: originalMessage.id
          };
        }
        // 检查是否为 Ark 消息（msg_type: 3）
        else if (parsedContent.msg_type === 3 && parsedContent.ark) {
          isSpecialMessage = true;
          // 私聊 Ark 消息需要 msg_id，才能作为回复消息而不是主动消息
          messageData = {
            msg_type: 3,
            ark: parsedContent.ark,
            msg_id: originalMessage.id
          };
        }
        // 检查是否为 Media 富媒体消息（msg_type: 7，包括图片）
        else if (parsedContent.msg_type === 7 && parsedContent.media) {
          isSpecialMessage = true;
          // 私聊 Media 消息需要 msg_id，才能作为回复消息而不是主动消息
          messageData = {
            msg_type: 7,
            media: parsedContent.media,
            msg_id: originalMessage.id
          };
        }
        // 常规文本消息或其他格式
        else {
          messageData = {
            content: content,
            msg_type: 0,
            msg_id: originalMessage.id
          };
        }
      } catch (e) {
        // 如果不是JSON，作为普通文本处理
        messageData = {
          content: content,
          msg_type: 0,
          msg_id: originalMessage.id
        };
      }

      console.log('消息类型:', isSpecialMessage ? 'Markdown/Ark/Media' : '文本');
      console.log('消息数据:', JSON.stringify(messageData));

      const response = await axios.post(`${this.apiBaseUrl}/v2/users/${originalMessage.author.user_openid}/messages`, messageData, {
        headers
      });

      console.log('✅ 私聊消息发送成功:', response.data);
      return response.data;
    } catch (error) {
      console.error('发送私聊消息失败:', error.response?.data || error.message);
      throw error;
    }
  }

  // 回复频道私聊消息（DIRECT_MESSAGE）
  async replyDirectMessage(originalMessage, content) {
    try {
      console.log('发送频道私聊回复消息...');
      console.log('原始消息:', JSON.stringify(originalMessage));
      const headers = await this.createHeaders();

      // 解析内容为JSON对象以检测是否为特殊消息类型
      let messageData = {};
      let isSpecialMessage = false;
      
      try {
        const parsedContent = JSON.parse(content);
        
        // 检查是否为 Markdown 消息（msg_type: 2）
        if (parsedContent.msg_type === 2 && parsedContent.markdown) {
          isSpecialMessage = true;
          // 频道私聊 Markdown 消息需要 msg_id，才能作为回复消息而不是主动消息
          messageData = {
            msg_type: 2,
            markdown: parsedContent.markdown,
            msg_id: originalMessage.id
          };
        }
        // 检查是否为 Ark 消息（msg_type: 3）
        else if (parsedContent.msg_type === 3 && parsedContent.ark) {
          isSpecialMessage = true;
          // 频道私聊 Ark 消息需要 msg_id，才能作为回复消息而不是主动消息
          messageData = {
            msg_type: 3,
            ark: parsedContent.ark,
            msg_id: originalMessage.id
          };
        }
        // 检查是否为 Media 富媒体消息（msg_type: 7，包括图片）
        else if (parsedContent.msg_type === 7 && parsedContent.media) {
          isSpecialMessage = true;
          // 频道私聊 Media 消息需要 msg_id，才能作为回复消息而不是主动消息
          messageData = {
            msg_type: 7,
            media: parsedContent.media,
            msg_id: originalMessage.id
          };
        }
        // 常规文本消息或其他格式
        else {
          messageData = {
            content: content,
            msg_id: originalMessage.id
          };
        }
      } catch (e) {
        // 如果不是JSON，作为普通文本处理
        messageData = {
          content: content,
          msg_id: originalMessage.id
        };
      }

      console.log('消息类型:', isSpecialMessage ? 'Markdown/Ark/Media' : '文本');
      console.log('消息数据:', JSON.stringify(messageData));

      // DIRECT_MESSAGE_CREATE 的回复方式：使用 /dms/{guild_id}/messages
      // guild_id 是频道ID，频道私聊就是用这个ID来回复
      const url = `${this.apiBaseUrl}/dms/${originalMessage.guild_id}/messages`;
      console.log('发送URL:', url);

      const response = await axios.post(url, messageData, {
        headers
      });

      console.log('✅ 频道私聊消息发送成功:', response.data);
      return response.data;
    } catch (error) {
      console.error('发送频道私聊消息失败');
      console.error('错误状态码:', error.response?.status);
      console.error('错误响应:', error.response?.data);
      console.error('错误信息:', error.message);
      throw error;
    }
  }

  // 启动心跳
  startHeartbeat(interval) {
    console.log(`启动心跳，间隔: ${interval}ms`);

    // 清理之前的心跳
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }

    // 设置新的心跳
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat();
    }, interval);
  }

  // 转发消息到SpringBoot
  async forwardToSpringBoot(eventType, data) {
    try {
      const response = await axios.post(`${this.springBootUrl}/qq/process-message`, {
        eventType: eventType,
        data: data,
        timestamp: Date.now()
      }, {
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': 'QQ-Bot-Direct-Service'
        },
        timeout: 25000
      });

      return response.data;
    } catch (error) {
      console.error('转发消息到SpringBoot失败:', error.message);
      return null;
    }
  }

  // 通知SpringBoot事件
  async notifySpringBoot(eventType, data) {
    try {
      await axios.post(`${this.springBootUrl}/qq/event-notification`, {
        eventType: eventType,
        data: data,
        timestamp: Date.now()
      }, {
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': 'QQ-Bot-Direct-Service'
        },
        timeout: 25000
      });
    } catch (error) {
      console.error('通知SpringBoot失败:', error.message);
    }
  }

  // 清理资源
  cleanup() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  // 初始化机器人
  async initialize() {
    try {
      console.log('正在初始化QQ机器人直接服务...');

      // 验证配置
      if (!this.botConfig.appID || !this.botConfig.clientSecret) {
        throw new Error('缺少必要的机器人配置: appID 和 clientSecret');
      }

      // 获取AccessToken
      await this.getAccessToken();

      // 获取机器人信息
      await this.getBotInfo();

      // 连接WebSocket
      await this.connectWebSocket();

      console.log('✅ QQ机器人直接服务初始化成功!');

    } catch (error) {
      console.error('初始化QQ机器人失败:', error);
      throw error;
    }
  }

  // 启动服务
  async start() {
    try {
      console.log('启动QQ机器人直接服务...');
      await this.initialize();

      // 处理进程退出
      process.on('SIGINT', () => {
        console.log('收到SIGINT信号，正在关闭服务...');
        this.cleanup();
        if (this.ws) {
          this.ws.close();
        }
        process.exit(0);
      });

      process.on('SIGTERM', () => {
        console.log('收到SIGTERM信号，正在关闭服务...');
        this.cleanup();
        if (this.ws) {
          this.ws.close();
        }
        process.exit(0);
      });

      console.log('🚀 QQ机器人服务运行中...');

    } catch (error) {
      console.error('启动QQ机器人服务失败:', error);
      process.exit(1);
    }
  }
}

// 启动服务
if (require.main === module) {
  const bot = new QQBotDirectService();
  bot.start();
}

module.exports = QQBotDirectService;