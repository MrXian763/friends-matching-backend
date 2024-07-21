package com.ziye.yupao.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

//@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test() {

        // List，数据存在本地 JVM 内存中
        List<String> list = new ArrayList<>();
        list.add("zicai");
        list.get(0);
        System.out.println("list:" + list.get(0));
        list.remove(0);

        // 数据存在 redis 的内存中
        RList<Object> rList = redissonClient.getList("test-list");
        rList.add("zicai");
        System.out.println("rList:" + rList.get(0));
        rList.remove(0);

    }

    @Test
    void testWatchDog() {
        RLock lock = redissonClient.getLock("zicai:precache:docache:lock");
        // 等待时间 释放时间 单位
        try {
            // 只有一个线程能获取到锁
            if (lock.tryLock(0, 30000L, TimeUnit.MILLISECONDS)) {
                Thread.sleep(100000); // 测试看门狗机制 key自动续期
                System.out.println("getLock: " + Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) { // 判断当前这个锁是不是这个线程的
                System.out.println("unlock: " + Thread.currentThread().getId());
                lock.unlock(); // 释放锁
            }
        }
    }
}
