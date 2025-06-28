package com.mumuk.global.config;


import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("오늘 뭐먹지? Swagger")
                .description("'오늘 뭐먹지' 팀의 Swagger 입니다.")
                .version("1.0");
    }
}
