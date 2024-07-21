package com.ziye.yupao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.ziye.yupao.mapper")
@EnableScheduling // 开启定时任务
public class ZiYeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZiYeApplication.class, args);
    }

}
