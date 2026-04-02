package com.segroup8.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SEGroup8 平台 API")
                .description("购物与二手交易平台基础接口文档")
                .version("0.0.1")
                .contact(new Contact().name("SEGroup8"))
        );
    }
}
