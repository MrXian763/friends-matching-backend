package com.ziye.yupao.service;

/**
 * 发送邮件服务接口
 *
 * @author xianziye
 */
public interface SendMailService {

    /**
     * 发送邮件
     * @param to 收件人
     * @param code 验证码
     */
    void sendMail(String to, String code);

}
