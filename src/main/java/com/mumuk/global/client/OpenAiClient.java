package com.mumuk.global.client;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final String model;

    // WebClient를 주입받고, 모델을 초기화
    public OpenAiClient(WebClient webClient, @Value("${openai.api.model}") String model) {
        this.webClient = webClient;
        this.model = model;
    }

    public String chat(String prompt) {
        Map<String, Object> body = createRequestBody(prompt);

        // 동기적으로 WebClient 를 사용하여 요청 보내기
        ResponseEntity<Map> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .toEntity(Map.class)
                .block();             // block()을 사용하여 동기적으로 응답을 기다림

        return extractContent(response.getBody());  // 응답 내용 처리
    }

    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        body.put("messages", messages);
        return body;
    }

    private String extractContent(Map<String, Object> response) {
        if (response == null || !response.containsKey("choices")) {
            throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices.isEmpty()) {
            throw new BusinessException(ErrorCode.OPENAI_NO_CHOICES);
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null || !message.containsKey("content")) {
            throw new BusinessException(ErrorCode.OPENAI_MISSING_CONTENT);
        }

        return (String) message.get("content");
    }
}
