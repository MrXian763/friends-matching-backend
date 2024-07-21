package com.ziye.yupao.job;

import com.ziye.yupao.model.domain.ChatMessages;
import com.ziye.yupao.service.ChatMessagesService;
import com.ziye.yupao.ws.ChatEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 定时任务持久化缓存中的消息
 *
 * @author xianziye
 */
@Component
@Slf4j
public class MessageCacheEndurance {

    @Resource
    private ChatMessagesService chatMessagesService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(cron = "0 0 4 * * ? ")
    @Transactional
    public void doMessageCacheEndurance() {
        log.info("开始持久化用户消息");
        HashSet<String> chatUserKeys = ChatEndpoint.chatUserKeys;
        for (String chatUserKey : chatUserKeys) {
            List<ChatMessages> chatMessagesList = new ArrayList<>();
            Set<String> messageKeys = redisTemplate.opsForList().getOperations().keys(chatUserKey);
            if (messageKeys == null || messageKeys.isEmpty()) {
                return;
            }

            String messageKey = null;
            for (String key : messageKeys) {
                messageKey = key;
            }

            Long size = redisTemplate.opsForList().size(messageKey);
            if (size == null || size <= 0) {
                continue;
            }
            List<Object> resultMessages = redisTemplate.opsForList().range(messageKey, 0, size);
            if (resultMessages == null || resultMessages.isEmpty()) {
                continue;
            }
            for (Object resultMessage : resultMessages) {
                ChatMessages chatMessages = new ChatMessages();
                BeanUtils.copyProperties(resultMessage, chatMessages);
                chatMessagesList.add(chatMessages);
            }
            boolean saveResult = chatMessagesService.saveBatch(chatMessagesList);
            if (saveResult) {
                redisTemplate.delete(messageKey);
            }
        }
        ChatEndpoint.chatUserKeys.clear();
        log.info("完成持久化用户消息");
    }

}
