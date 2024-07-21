package com.ziye.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.mapper.ChatMessagesMapper;
import com.ziye.yupao.model.domain.ChatMessages;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.vo.ChatMessagesVO;
import com.ziye.yupao.service.ChatMessagesService;
import com.ziye.yupao.service.UserService;
import com.ziye.yupao.ws.pojo.ResultMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xianziye
 * @description 针对表【chat_messages(用户私聊消息表)】的数据库操作Service实现
 * @createDate 2024-06-18 11:50:07
 */
@Service
public class ChatMessagesServiceImpl extends ServiceImpl<ChatMessagesMapper, ChatMessages>
        implements ChatMessagesService {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public static User loginUser;

    /**
     * 获取消息状态
     *
     * @param receiveUser 接收消息用户
     * @param sendUser    发送消息用户
     * @return true-有未读消息 false-无未读消息
     */
    @Override
    public boolean getReadStatus(User sendUser, User receiveUser) {
        if (receiveUser == null || sendUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        // 获取当前登录用户接受的消息
        long receiveUserId = receiveUser.getId();
        long sendUserId = sendUser.getId();
        QueryWrapper<ChatMessages> chatMessagesQueryWrapper = new QueryWrapper<>();
        chatMessagesQueryWrapper.eq("sender_id", sendUserId);
        chatMessagesQueryWrapper.eq("receiver_id", receiveUserId);
        chatMessagesQueryWrapper.orderByDesc("timestamp"); // 根据时间降序排序
        List<ChatMessages> chatMessages = this.list(chatMessagesQueryWrapper);
        if (chatMessages == null || chatMessages.isEmpty()) { // 没有消息则返回false，表示没有未读消息
            return false;
        }

        // 读取缓存中的消息
        chatMessages.addAll(getChatMessagesFromCache(sendUserId, receiveUserId, true));

        // 消息根据发送时间降序排序
        List<ChatMessages> chatMessagesList = chatMessages.stream()
                .sorted((c1, c2) -> {
                    long time = c2.getTimestamp().getTime() - c1.getTimestamp().getTime();
                    if (time == 0) return 0;
                    if (time > 0) return 1;
                    return -1;
                })
                .collect(Collectors.toList());

        // 获取最新的消息
        if (chatMessagesList.isEmpty()) {
            return false;
        }
        ChatMessages messages = chatMessagesList.get(0);
        if (messages == null || messages.getReadStatus() == null || messages.getReadStatus() == 0) {
            return true;
        }
        // 状态为未读则返回true
        return false;
    }

    /**
     * 获取历史消息
     *
     * @param senderId   发送者id
     * @param receiverId 接收者id
     * @return 消息集合
     */
    @Override
    @Transactional
    public List<ChatMessagesVO> getHistoryMessages(long senderId, long receiverId) {
        if (senderId <= 0 || receiverId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<ChatMessages> chatMessagesQueryWrapper = new QueryWrapper<>();
        chatMessagesQueryWrapper
                .eq("sender_id", senderId).eq("receiver_id", receiverId)
                .or()
                .eq("sender_id", receiverId).eq("receiver_id", senderId);
        List<ChatMessages> chatMessages = this.list(chatMessagesQueryWrapper);

        // 设置消息已读状态
        boolean flag = false;
        // 检查数据库中的消息状态
        for (ChatMessages chatMessage : chatMessages) {
            if (chatMessage.getReadStatus() == null ||
                    (chatMessage.getReceiverId() == receiverId && chatMessage.getReadStatus() == 0)) {
                chatMessage.setReadStatus(1);
                flag = true;
            }
        }
        if (flag) { // 更新数据库中的消息已读状态
            this.updateBatchById(chatMessages);
        }

        // 获取缓存中的聊天消息
        List<ChatMessages> cacheChatMessages =
                (List<ChatMessages>) this.getChatMessagesFromCache(senderId, receiverId, false);

        // 更新缓存中的消息状态
        String key1 = "chat_messages:sender_id:" + senderId + ":receiver_id:" + receiverId;
        String key2 = "chat_messages:sender_id:" + receiverId + ":receiver_id:" + senderId;

        // 先清空旧的缓存数据
        redisTemplate.delete(key1);
        redisTemplate.delete(key2);

        // 更新消息状态并重新插入缓存
        for (ChatMessages cacheChatMessage : cacheChatMessages) {
            String key = "chat_messages:sender_id:" + cacheChatMessage.getSenderId() +
                    ":receiver_id:" + cacheChatMessage.getReceiverId();
            if (cacheChatMessage.getReadStatus() == null ||
                    (cacheChatMessage.getReceiverId() == receiverId && cacheChatMessage.getReadStatus() == 0)) {
                cacheChatMessage.setReadStatus(1);
            }
            ResultMessage resultMessage = new ResultMessage();
            BeanUtils.copyProperties(cacheChatMessage, resultMessage);
            redisTemplate.opsForList().rightPush(key, resultMessage);
        }

        if (!cacheChatMessages.isEmpty()) {
            chatMessages.addAll(cacheChatMessages);
        }

        List<ChatMessagesVO> chatMessagesVOList = new ArrayList<>();
        chatMessages.forEach(message -> {
            User senderUserId = userService.getById(message.getSenderId()); // 根据id获取发消息用户
            String senderUserAvatar = userService.getById(senderUserId).getAvatarUrl(); // 设置发消息的用户头像
            ChatMessagesVO chatMessagesVO = new ChatMessagesVO();
            chatMessagesVO.setSenderId(message.getSenderId()); // 设置发送者id
            chatMessagesVO.setReceiverId(String.valueOf(message.getReceiverId())); // 设置接收者id
            chatMessagesVO.setAvatar(senderUserAvatar); // 设置发送者头像
            chatMessagesVO.setMessage(message.getMessage()); // 设置消息
            chatMessagesVO.setTimestamp(message.getTimestamp()); // 设置时间
            chatMessagesVOList.add(chatMessagesVO);
        });

        // 根据时间升序排序返回
        return chatMessagesVOList.stream()
                .sorted((c1, c2) -> {
                    long time = c1.getTimestamp().getTime() - c2.getTimestamp().getTime();
                    if (time == 0) return 0;
                    if (time > 0) return 1;
                    return -1;
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新已读消息
     *
     * @param chatIds 私聊用户组id集合
     */
    @Override
    public boolean updateUnReadMsg(List<Map<Long, Long>> chatIds) {
        if (chatIds == null) return false;
        QueryWrapper<ChatMessages> chatMessagesQueryWrapper;
        ChatMessages chatMessages = new ChatMessages();
        for (Map<Long, Long> chatId : chatIds) {
            for (Long senderId : chatId.keySet()) {
                long receiverId = chatId.get(senderId);
                long loginUserId = loginUser.getId();
                if (receiverId != loginUserId) return false; // 接收者查看了消息才设置消息已读
                chatMessagesQueryWrapper = new QueryWrapper<>();
                chatMessagesQueryWrapper
                        .eq("sender_id", senderId).eq("receiver_id", receiverId)
                        .ne("read_status", 1)
                        .or()
                        .eq("sender_id", receiverId).eq("receiver_id", senderId)
                        .ne("read_status", 1);
                chatMessages.setReadStatus(1);
                this.update(chatMessages, chatMessagesQueryWrapper);
            }
        }
        return true;
    }

    /**
     * 持久化消息记录
     *
     * @param messageObjs 消息对象集合
     */
    @Override
    @Transactional
    public void saveMessages(List<ResultMessage> messageObjs) {
        // 没消息记录直接返回
        if (messageObjs == null || messageObjs.isEmpty()) return;

        // 批量保存聊天记录
        List<ChatMessages> chatMessagesList = new ArrayList<>();
        for (ResultMessage messageObj : messageObjs) {
            ChatMessages chatMessages = new ChatMessages();
            chatMessages.setSenderId((int) messageObj.getSenderId());
            chatMessages.setReceiverId((int) messageObj.getReceiverId());
            chatMessages.setMessage(messageObj.getMessage());
            chatMessages.setTimestamp(messageObj.getTimestamp());
            chatMessages.setReadStatus(messageObj.getReadStatus());
            chatMessagesList.add(chatMessages);
        }
        this.saveBatch(chatMessagesList);
    }

    /**
     * 获取缓存中的信息记录
     *
     * @param senderId        发送者 id
     * @param receiverId      接收者 id
     * @param isGetReadStatus 是否获取消息已读状态
     * @return
     */
    private Collection<? extends ChatMessages> getChatMessagesFromCache(long senderId,
                                                                        long receiverId, boolean isGetReadStatus) {
        List<ChatMessages> chatMessages = new ArrayList<>();
        Long size1 = redisTemplate.opsForList()
                .size("chat_messages:sender_id:" + senderId + ":receiver_id:" + receiverId);
        if (size1 != null && size1 > 0) {
            List<Object> resultMessages = redisTemplate.opsForList()
                    .range("chat_messages:sender_id:" + senderId + ":receiver_id:" + receiverId, 0, size1);
            if (resultMessages != null && !resultMessages.isEmpty()) { // 缓存中有消息
                for (Object resultMessage : resultMessages) {
                    ChatMessages chatMessage = new ChatMessages();
                    ResultMessage resultMessage1 = (ResultMessage) resultMessage;
                    BeanUtils.copyProperties(resultMessage1, chatMessage);
                    chatMessages.add(chatMessage);
                }
            }
        }
        if (!isGetReadStatus) {
            Long size2 = redisTemplate.opsForList()
                    .size("chat_messages:sender_id:" + receiverId + ":receiver_id:" + senderId);
            if (size2 != null && size2 > 0) {
                List<Object> resultMessages = redisTemplate.opsForList()
                        .range("chat_messages:sender_id:" + receiverId + ":receiver_id:" + senderId, 0, size2);
                if (resultMessages != null && !resultMessages.isEmpty()) { // 缓存中有消息
                    for (Object resultMessage : resultMessages) {
                        ChatMessages chatMessage = new ChatMessages();
                        ResultMessage resultMessage1 = (ResultMessage) resultMessage;
                        BeanUtils.copyProperties(resultMessage1, chatMessage);
                        chatMessages.add(chatMessage);
                    }
                }
            }
        }
        return chatMessages.isEmpty() ? new ArrayList<>() : chatMessages;
    }
}




