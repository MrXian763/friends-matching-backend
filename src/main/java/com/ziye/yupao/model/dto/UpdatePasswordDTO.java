package com.ziye.yupao.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新密码请求体
 */
@Data
public class UpdatePasswordDTO implements Serializable {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认密码
     */
    private String confirmPassword;
}
