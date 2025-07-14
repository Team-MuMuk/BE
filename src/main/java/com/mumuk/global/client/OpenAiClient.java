package com.mumuk.global.client;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final String model;

    public OpenAiClient(WebClient webClient, @Value("${openai.model}") String model) {
        this.webClient = webClient;
        this.model = model;
    }

    public Mono<String> chat(String prompt) {
        Map<String, Object> body = createRequestBody(prompt);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.isError(),  // 오류 여부 체크
                        this::handleError            // 오류 처리 메서드
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(this::extractContent);
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

    private Mono<? extends Throwable> handleError(ClientResponse clientResponse) {
        if (clientResponse.statusCode().equals(HttpStatus.UNAUTHORIZED)) {
            return Mono.error(new BusinessException(ErrorCode.OPENAI_INVALID_API_KEY));  // 401 오류 처리
        } else if (clientResponse.statusCode().equals(HttpStatus.SERVICE_UNAVAILABLE)) {
            return Mono.error(new BusinessException(ErrorCode.OPENAI_SERVICE_UNAVAILABLE));  // 503 오류 처리
        } else {
            // 기타 오류 처리 (로깅 또는 다른 처리가 필요할 경우)
            return Mono.error(new BusinessException(ErrorCode.OPENAI_API_ERROR));  // 기본 API 오류 처리
        }
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
