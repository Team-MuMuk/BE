package com.mumuk.global.client;

import com.mumuk.global.util.FileResourceUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.UUID;


@Component
public class ClovaOcrClient {

    @Value("${naver.clova.ocr.invoke-url}")
    private String invokeUrl;

    @Value("${naver.clova.ocr.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate;

    public ClovaOcrClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String callClovaOcr(MultipartFile imageFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-OCR-SECRET", secretKey);

            String messageJson = buildClovaRequestMessage(imageFile);


            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", FileResourceUtil.toResource(imageFile));
            body.add("message", messageJson);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(invokeUrl, requestEntity, String.class);

            return response.getBody();
        } catch (IOException e) {
            throw new RuntimeException("CLOVA OCR 요청 실패", e);
        }
    }

    private String buildClovaRequestMessage(MultipartFile imageFile) {
        return """
        {
            "version": "V2",
            "requestId": "%s",
            "timestamp": %d,
            "images": [
                {
                    "format": "%s",
                    "name": "%s",
                    "templateIds": [38491]
                }
            ]
        }
        """.formatted(
                UUID.randomUUID(),
                System.currentTimeMillis(),
                getFileExtension(imageFile.getOriginalFilename()),  // "jpg", "png" 등
                imageFile.getOriginalFilename()
        );
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "jpg";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex != -1) ? filename.substring(dotIndex + 1).toLowerCase() : "jpg";
    }
}
