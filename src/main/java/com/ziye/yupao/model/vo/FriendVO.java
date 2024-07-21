package com.ziye.yupao.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 好友列表返回体
 *
 * @author xianziye
 */
@Data
public class FriendVO implements Serializable {

    /**
     * 好友id
     */
    private long id;

    /**
     * 好友昵称
     */
    private String name;

    /**
     * 状态 0-离线 1-在线
     */
    private Integer status;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 是否有未读消息
     */
    private boolean hasUnread;

}
