package com.mumuk.global.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String accessTokenSchemeName = "accessToken";
        String refreshTokenSchemeName = "refreshToken";

        // API 요청헤더에 인증정보 포함
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(accessTokenSchemeName);

        // SecuritySchemes 등록
        Components components = new Components()
                .addSecuritySchemes(accessTokenSchemeName, new SecurityScheme()
                        .name("accessToken")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(refreshTokenSchemeName, new SecurityScheme()
                        .name("refreshToken")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .components(components)
                .info(apiInfo())
                .addServersItem(new Server().url("/"))
                .addSecurityItem(securityRequirement);
    }

    private Info apiInfo() {
        return new Info()
                .title("✨오늘 뭐 해먹지💡? Swagger")
                .description("오늘 뭐 해먹지 팀의 Swagger 입니다.")
                .version("1.0");
    }
}
