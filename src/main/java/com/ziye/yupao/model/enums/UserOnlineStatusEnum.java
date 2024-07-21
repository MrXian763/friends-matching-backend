package com.ziye.yupao.model.enums;

import lombok.Getter;

/**
 * 用户在线状态枚举
 *
 * @author zicai
 */
@Getter
public enum UserOnlineStatusEnum {

    OFFLINE("offline", "离线"),
    ONLINE("online", "在线");

    /**
     * 状态值
     */
    private String value;

    /**
     * 描述
     */
    private String text;

    UserOnlineStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static UserOnlineStatusEnum getEnumByValue(String value) {
        if (value == null) return null;
        UserOnlineStatusEnum[] values = UserOnlineStatusEnum.values();
        for (UserOnlineStatusEnum teamStatusEnum : values) {
            if (teamStatusEnum.getValue().equals(value)) {
                return teamStatusEnum;
            }
        }
        return null;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setText(String text) {
        this.text = text;
    }
    }
