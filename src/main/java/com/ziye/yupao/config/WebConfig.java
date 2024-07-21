package com.ziye.yupao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 跨域配置
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // todo 设置为你的信息
                .allowedOrigins("http://localhost:3000", "http://localhost:8000","your_domain.com") // 允许本机端口3000 8000来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法
                .maxAge(3600) // 预检请求的有效期
                .allowCredentials(true); // 允许携带凭证
    }
}
