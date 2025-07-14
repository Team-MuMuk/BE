package com.mumuk.global.config;


import com.mumuk.global.client.OpenAiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiClientConfig {

    @Value("${openai.api.url}")
    private String baseUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Bean
    public OpenAiClient openAiClient(WebClient webClient, @Value("${openai.api.model}") String model) {
        return new OpenAiClient(webClient, model);
    }
}
