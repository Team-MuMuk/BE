package com.mumuk.global.config;

import com.mumuk.global.client.GeminiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiClientConfig {

    @Value("${openai.api.url}")
    private String baseUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    @Bean
    public GeminiClient geminiClient(WebClient geminiWebClient, @Value("${gemini.api.model}") String model) {
        return new GeminiClient(geminiWebClient, model);
    }
}
