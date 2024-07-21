package com.ziye.yupao.service;

import com.ziye.yupao.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

//@SpringBootTest
public class InsertUserTest {

    @Resource
    private UserService userService;

    // 自定义线程池（参数：核心线程数 最大线程数 最大空闲时间 时间单位 阻塞队列）
    private ExecutorService executorService =
            new ThreadPoolExecutor(60, 1000, 10000,
                    TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 批量插入用户
     */
    @Test
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("假紫菜");
            user.setUserAccount("fakerzicai");
            user.setAvatarUrl("your_avatar_url");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setPhone("785436789998");
            user.setEmail("your_email@qq.com");
            user.setTags("[java]");
            user.setUserStatus(0);
            user.setIsDelete(0);
            user.setUserRole(0);
            user.setPlanetCode("11111");
            userList.add(user);
        }
        // 分批插入
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
        ;
    }

    /**
     * 并发批量插入用户（多线程）
     */
    @Test
    public void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch(); // 计时器
        stopWatch.start(); // 开始计时
        int batchSize = 10000; // 每批插入的用户数量
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>(); // 存储所有异步操作的Future列表
        for (int i = 0; i < 50; i++) { // 进行50次插入操作
            List<User> userList = new ArrayList<>(); // 存储即将插入的用户列表
            while (true) {
                j++;
                User user = new User();
                user.setUsername("假紫菜");
                user.setUserAccount("fakerzicai");
                user.setAvatarUrl("your_avatar_url");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("18025554581");
                user.setEmail("your_email@qq.com");
                user.setTags("[\"女\",\"前端\"]");
                user.setUserStatus(0);
                user.setIsDelete(0);
                user.setUserRole(0);
                user.setPlanetCode("11111");
                userList.add(user);
                if (j % batchSize == 0) {
                    break;
                }
            }
            // 异步执行插入操作
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize); // 批量保存用户
            });
            futureList.add(future); // 将此次异步操作的Future添加到列表中，便于后续跟踪任务完成情况
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join(); // 等待所有异步操作完成
        stopWatch.stop(); // 停止计时
        System.out.println(stopWatch.getTotalTimeMillis()); // 输出总耗时
    }

}
