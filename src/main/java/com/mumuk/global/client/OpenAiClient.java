package com.mumuk.global.client;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class OpenAiClient {

    private final String baseUrl;
    private final String model;

    public OpenAiClient(@Value("${openai.url}") String baseUrl, @Value("${openai.model}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String chat(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = createRequestBody(prompt);

        try {
            // POST 요청 보내기
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/chat/completions", body, Map.class);
            return extractContent(response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new BusinessException(ErrorCode.OPENAI_INVALID_API_KEY);
            } else if (e.getStatusCode().is5xxServerError()) {
                throw new BusinessException(ErrorCode.OPENAI_SERVICE_UNAVAILABLE);
            }
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
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
