package com.ziye.yupao.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 测试发送邮件
 *
 * @author xianziye
 */
@SpringBootTest
public class SendMailTest {

    @Resource
    private SendMailService sendMailService;

    @Test
    public void testSendMail() {
        String to = "your_email@qq.com";
        String code = "932023";
        sendMailService.sendMail(to, code);
    }

}
