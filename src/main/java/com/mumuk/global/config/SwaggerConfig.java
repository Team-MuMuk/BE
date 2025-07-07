package com.mumuk.global.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization"))) // Swagger UI에서 자동 주입
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .info(apiInfo());
    }


    private Info apiInfo() {
        return new Info()
                .title("✨오늘 뭐 해먹지💡? Swagger")
                .description("오늘 뭐 해먹지 팀의 Swagger 입니다.")
                .version("1.0");
    }
}
