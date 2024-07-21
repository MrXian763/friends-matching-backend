package com.ziye.yupao.service;

import com.ziye.yupao.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void test() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("zicaiString", "dog");
        valueOperations.set("zicaiInt", 1);
        valueOperations.set("zicaiDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("zicai");
        valueOperations.set("zicaiUser", user);

        // 查
        Object zicai = valueOperations.get("zicaiString");
        Assertions.assertTrue("dog".equals((String) zicai));
        zicai = valueOperations.get("zicaiInt");
        Assertions.assertTrue(1 == (Integer) zicai);
        zicai = valueOperations.get("zicaiDouble");
        Assertions.assertTrue(2.0 == (Double) zicai);
        System.out.println(valueOperations.get("zicaiUser"));

        // 改
        valueOperations.set("zicaiString", "zicaiDog");

        // 删
        redisTemplate.delete("zicaiInt");
    }

}
