package com.ziye.yupao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * 自定义 swagger 接口文档的配置
 *
 * @author zicai
 */
@Configuration
@EnableSwagger2WebMvc // Swagger的开关，表示已经启用Swagger
@Profile({"dev", "test"}) // 指定什么环境下接口文档生效，避免泄漏信息
public class SwaggerConfig {
    @Bean(value = "defalutApi2")
    public Docket api() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select() // 选择哪些路径和api会生成document
                .apis(RequestHandlerSelectors.basePackage("com.ziye.yupao.controller")) // 选择监控的package
                .paths(PathSelectors.any()) // 对所有路径进行监控
                .build();
        return docket;
    }

    /**
     * api 信息
     *
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("紫菜伙伴匹配")
                // todo 设置为你的信息
                .contact(new Contact("your_name", "your_url", "your_email"))
                .description("接口文档")
                // todo 设置为你的信息
                .termsOfServiceUrl("your_url")
                .version("1.0")
                .build();
    }
}
