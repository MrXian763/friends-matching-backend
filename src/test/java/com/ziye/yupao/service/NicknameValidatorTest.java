package com.ziye.yupao.service;

import com.ziye.yupao.utils.nickname.NicknameValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 用户昵称校验测试
 */
@SpringBootTest
public class NicknameValidatorTest {

    @Test
    void test() {
        String a = NicknameValidator.validateNickname("fwsf*&∞®¢∑ƒ†");
        String b = NicknameValidator.validateNickname("垃圾");
        String c = NicknameValidator.validateNickname("正常昵称");

    }
}
