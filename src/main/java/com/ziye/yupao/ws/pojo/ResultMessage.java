package com.ziye.yupao.ws.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息接收实体类
 */
@Data
public class ResultMessage implements Serializable {

    /**
     * 发送者id
     */
    private long senderId;

    /**
     * 接收者id
     */
    private long receiverId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 发送时间
     */
    private Date timestamp;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 状态：0-未读，1-已读
     */
    private Integer readStatus;

}
