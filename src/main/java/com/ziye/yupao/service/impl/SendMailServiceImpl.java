package com.ziye.yupao.service.impl;

import com.ziye.yupao.common.ErrorCode;
import com.ziye.yupao.exception.BussinessException;
import com.ziye.yupao.service.SendMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * 发送邮件服务接口实现类
 *
 * @author xianziye
 */
@Service
public class SendMailServiceImpl implements SendMailService {

    @Autowired
    private JavaMailSender javaMailSender;

    /**
     * 标题
     */
    public static final String subject = "伙伴匹配";

    /**
     * 发件人
     */
    // todo 设置为自己的邮箱
    public static final String from = "your_email@qq.com";

    /**
     * 重置密码
     * @param to 收件人邮箱
     * @param defaultPassword 随机密码
     */
    public void sendMail(String to, String defaultPassword) {
        String context = "伙伴匹配系统重置密码，已将你的密码重置为：" + defaultPassword;
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setSubject(subject);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setText(context);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "发送邮件失败");
        }
    }

}
