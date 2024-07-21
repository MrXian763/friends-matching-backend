package com.ziye.yupao.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 重置密码请求体
 *
 * @author xianziye
 */
@Data
public class ResetPasswordDTO implements Serializable {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 邮箱
     */
    private String email;

}
