package com.ziye.yupao.service;

import com.ziye.yupao.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 用户服务测试
 *
 * @author ziye
 */
@SpringBootTest
class UserServiceTest {

    @Resource
    private UserService userService;

    /**
     * 测试连接数据库操作
     */
    @Test
    void testAddUser() {
        User user = new User();
        user.setUsername("xianzhiyong");
        user.setUserAccount("xian");
        user.setAvatarUrl("your_avatar_url");
        user.setGender(0);
        user.setUserPassword("123");
        user.setPhone("456");
        user.setEmail("789");

        boolean result = userService.save(user);
        Assertions.assertTrue(result);
        System.out.println(user.getId());

    }

    /**
     * 测试密码加密
     */
    @Test
    void testDigest() {
        String newPassword = DigestUtils.md5DigestAsHex(("abcd" + "mypassword").getBytes());
        System.out.println(newPassword);
    }

    /**
     * 测试用户注册逻辑
     */
    @Test
    void userRegister() {
        String username = "sofio";
        String userAccount = "yong";
        String userPassword = "";
        String checkPassword = "123456";
        String planetCode = "31000";
        String email = "your_email@qq.com";

        // 密码为空
        long result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertEquals(-1, result);

        // 用户名小于四位
        userAccount = "yo";
        result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertEquals(-1, result);

        // 密码小于八位
        userAccount = "yong";
        userPassword = "123456";
        result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertEquals(-1, result);

        // 用户名包含特殊字符
        userAccount = "yo ng";
        userPassword = "123455678";
        checkPassword = "12345678";
        result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertEquals(-1, result);

        // 密码不一致
        userAccount = "yong";
        userPassword = "123456789";
        result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertEquals(-1, result);

        // 校用户名重复
        userAccount = "xian";
        checkPassword = "123456789";
        result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertEquals(-1, result);

        // 校验星球编号重复
        userAccount = "yong";
        result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertEquals(-1, result);

        // 合法情况
        result = userService.userRegister(username, userAccount, userPassword, checkPassword, planetCode, email);
        Assertions.assertTrue(result > 0);
    }

    /**
     * 测试根据标签搜索用户
     */
    @Test
    public void testSearchByTags() {
        List<String> tagNameList = Arrays.asList("java", "python");
        List<User> userList = userService.searchUsersByTags(tagNameList);
        Assert.assertNotNull(userList);
    }
}