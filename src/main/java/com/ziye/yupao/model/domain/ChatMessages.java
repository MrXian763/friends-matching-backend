package com.ziye.yupao.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户私聊消息表
 * @TableName chat_messages
 */
@TableName(value ="chat_messages")
@Data
public class ChatMessages implements Serializable {
    /**
     * 唯一标识
     */
    @TableId(value = "chat_id", type = IdType.AUTO)
    private long chatId;

    /**
     * 发送者id
     */
    @TableField(value = "sender_id")
    private long senderId;

    /**
     * 接收者id
     */
    @TableField(value = "receiver_id")
    private long receiverId;

    /**
     * 消息内容
     */
    @TableField(value = "message")
    private String message;

    /**
     * 发送时间
     */
    @TableField(value = "timestamp")
    private Date timestamp;

    /**
     * 状态：0-未读，1-已读
     */
    @TableField(value = "read_status")
    private Integer readStatus;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}