package com.ziye.yupao.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息返回体
 *
 * @author xianziye
 */
@Data
public class ChatMessagesVO implements Serializable {

    /**
     * 发送者id
     */
    private long senderId;

    /**
     * 接收者id
     */
    private String receiverId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private Date timestamp;

    /**
     * 头像url
     */
    private String avatar;

}
