package com.ziye.yupao.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.Set;

@SpringBootTest
public class UpdateUserStatusTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void test() {
        // 搜索键
        Set<String> keys = stringRedisTemplate.keys("user:onlineStatus*");
        if (keys != null) {
            for (String key : keys) {
                System.out.println(key + " -> " + stringRedisTemplate.opsForValue().get(key));
                System.out.println(key.substring(18));
            }
        } else {
            System.out.println("No keys found.");
        }
    }

    @Test
    void test2() {
        redisTemplate.opsForHash().put("key", "19199999999", "2");
        Object o = redisTemplate.opsForHash().get("key", "19199999999");
        System.out.println(o);
    }
}
