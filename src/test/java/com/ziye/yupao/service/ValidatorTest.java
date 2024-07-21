package com.ziye.yupao.service;

import com.ziye.yupao.utils.StringValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 非法校验测试
 *
 * @author xianziye
 */
@SpringBootTest
public class ValidatorTest {

    /**
     * 测试QQ邮箱
     */
    @Test
    public void testValidateEmail() {
        String email1 = "231452342314@qq.com";
        String email2 = "23rkjk∆˚∆˚@qqsdf.com";
        String email3 = "¬ø∆ø∆ø@qq.sdfcom";
        String email4 = "tqwesdafas@@@@qq.com";
        String email5 = "647668中文834@qq.com";
        Assertions.assertTrue(StringValidator.isValidQQEmail(email1));
        Assertions.assertFalse(StringValidator.isValidQQEmail(email2));
        Assertions.assertFalse(StringValidator.isValidQQEmail(email3));
        Assertions.assertFalse(StringValidator.isValidQQEmail(email4));
        Assertions.assertFalse(StringValidator.isValidQQEmail(email5));
    }

}
