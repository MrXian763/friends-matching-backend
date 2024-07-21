package com.ziye.yupao.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ziye.yupao.model.domain.ChatMessages;
import com.ziye.yupao.model.domain.User;
import com.ziye.yupao.model.vo.ChatMessagesVO;
import com.ziye.yupao.ws.pojo.ResultMessage;

import java.util.List;
import java.util.Map;

/**
* @author xianziye
* @description 针对表【chat_messages(用户私聊消息表)】的数据库操作Service
* @createDate 2024-06-18 11:50:07
*/
public interface ChatMessagesService extends IService<ChatMessages> {

    /**
     * 获取消息状态
     * @param receiveUser 接收消息用户
     * @param sendUser 发送消息用户
     * @return true-有未读消息 false-无未读消息
     */
    boolean getReadStatus(User sendUser, User receiveUser);

    /**
     * 获取历史消息
     * @param senderId 发送者id
     * @param receiverId 接收者id
     * @return 消息集合
     */
    List<ChatMessagesVO> getHistoryMessages(long senderId, long receiverId);

    /**
     * 更新已读消息
     * @param chatIds 私聊用户组集合
     */
    boolean updateUnReadMsg(List<Map<Long, Long>> chatIds);

    /**
     * 持久化消息记录
     * @param messageObjs 消息对象集合
     */
    void saveMessages(List<ResultMessage> messageObjs);
}
