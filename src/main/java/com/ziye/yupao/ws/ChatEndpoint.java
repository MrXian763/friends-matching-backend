package com.ziye.yupao.ws;

import com.alibaba.fastjson.JSON;
import com.ziye.yupao.config.GetHttpSessionConfig;
import com.ziye.yupao.contant.UserConstant;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.service.ChatMessagesService;
import com.ziye.yupao.service.impl.ChatMessagesServiceImpl;
import com.ziye.yupao.ws.pojo.ResultMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat", configurator = GetHttpSessionConfig.class)
@Component
public class ChatEndpoint {

    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    private static final List<Map<Long, Long>> chatIds = new ArrayList<>();

    public static HashSet<String> chatUserKeys = new HashSet<>();

    private HttpSession httpSession;

    private static RedisTemplate redisTemplate;

    @Autowired
    public void setYourService(RedisTemplate redisTemplate) {
        ChatEndpoint.redisTemplate = redisTemplate;
    }

    // 静态方法获取Bean的示例
    private static RedisTemplate getRedisTemplate() {
        if (redisTemplate == null) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(ChatEndpoint.class);
        }
        return redisTemplate;
    }

    private static ChatMessagesService chatMessagesService;

    @Autowired
    public void setChatMessagesService(ChatMessagesService chatMessagesService) {
        ChatEndpoint.chatMessagesService = chatMessagesService;
    }

    // 静态方法获取Bean的示例
    private static ChatMessagesService getChatMessagesService() {
        if (chatMessagesService == null) {
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(ChatEndpoint.class);
        }
        return chatMessagesService;
    }

    /**
     * 连接成功时触发
     *
     * @param session
     * @param config
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 存储会话信息
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        User user = (User) this.httpSession.getAttribute(UserConstant.USER_LOGIN_STATE);
        ChatMessagesServiceImpl.loginUser = user;
        long userId = user.getId();
        onlineUsers.put(userId, session);
        ChatMessagesService chatMessagesService = getChatMessagesService();

        // 标识消息已读
        boolean updateResult = chatMessagesService.updateUnReadMsg(chatIds);
        if (updateResult) chatIds.clear();
    }

    /**
     * 收到消息时触发
     *
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {
        try {
            // 将收到的字符串消息转换为 ResultMessage 对象
            ResultMessage msg = JSON.parseObject(message, ResultMessage.class);

            long receiverId = msg.getReceiverId(); // 接收者id
            long senderId = msg.getSenderId(); // 发送者id

            // 记录当前聊天用户
            String key = "chat_messages:sender_id:" + senderId + ":receiver_id:" + receiverId;
            chatUserKeys.add(key);

            // 存储当前聊天的用户组，用于标识已读消息
            Map<Long, Long> chatId = new HashMap<>();
            chatId.put(senderId, receiverId);
            chatIds.add(chatId);


            // 创建一个新的 ResultMessage 对象，并将 msg 的属性复制给 resultMsg
            ResultMessage resultMsg = new ResultMessage();
            BeanUtils.copyProperties(msg, resultMsg);

            // 从在线用户列表中获取接收者的 WebSocket 会话
            Session session1 = onlineUsers.get(receiverId);
            Session session2 = onlineUsers.get(senderId);
            boolean flag = true;
            if (session1 != null && session2 != null) {
                if (resultMsg.getReceiverId() == receiverId && resultMsg.getSenderId() == senderId) { // 对方用户也在线
                    String resultMessage = JSON.toJSONString(resultMsg);
                    // 通过 WebSocket 发送消息给接收者
                    session1.getBasicRemote().sendText(resultMessage);
                    msg.setReadStatus(1); // 连接中对话的消息为已读
                    flag = false;
                }
            }
            if (flag) {
                msg.setReadStatus(0); // 连接中对话的消息为未读
            }
            // 将消息存入 Redis 缓存
            RedisTemplate redisTemplate = getRedisTemplate();
            redisTemplate.opsForList().rightPush(key, msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接关闭时触发
     *
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        // 移除登录态
        User user = (User) this.httpSession.getAttribute(UserConstant.USER_LOGIN_STATE);
        long userId = user.getId();
        onlineUsers.remove(userId);
    }

    /**
     * 发生错误时触发
     *
     * @param session
     * @param throwable
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }


}
