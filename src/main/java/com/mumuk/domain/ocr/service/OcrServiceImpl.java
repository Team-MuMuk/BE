package com.mumuk.domain.ocr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.ocr.entity.UserHealthData;
import com.mumuk.domain.ocr.repository.UserHealthDataRepository;
import com.mumuk.global.client.ClovaOcrClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private final UserHealthDataRepository userHealthDataRepository;
    private final ClovaOcrClient clovaOcrClient;
    private final ObjectMapper objectMapper;

    public OcrServiceImpl(UserHealthDataRepository userHealthDataRepository, ClovaOcrClient clovaOcrClient, ObjectMapper objectMapper) {
        this.userHealthDataRepository = userHealthDataRepository;
        this.clovaOcrClient = clovaOcrClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, String> extractText(MultipartFile imageFile) {

        String ocrJson = clovaOcrClient.callClovaOcr(imageFile);
        Map<String, String> result = new LinkedHashMap<>();

        Map<String, String> keyNameMap = Map.of(
                "체수분", "체수분 (L)",
                "단백질", "단백질 (kg)",
                "무기질", "무기질",
                "체지방", "체지방 (kg)",
                "체중", "체중 (kg)",
                "골격근량", "골격근량",
                "체지방량", "체지방량 (kg)",
                "BMI", "BMI",
                "체지방률", "체지방률"
        );

        Map<String, String> overrideKeys = Map.of(
                "Weight", "체중 (kg)",
                "Skeletal Muscle Mass", "골격근량",
                "Body FatMass", "체지방량 (kg)",
                "Protein", "단백질 (kg)",
                "Body Fat", "체지방 (kg)",
                "Total Body Water", "체수분 (L)",
                "Minerals", "무기질"
        );

        Set<String> allowedKeys = keyNameMap.keySet();

        try {
            JsonNode fields = objectMapper
                    .readTree(ocrJson)
                    .path("images").get(0)
                    .path("fields");

            if (fields.size() == 0) return result;

            String rawText = fields.get(0).path("inferText").asText();
            String[] lines = rawText.split("\n");

            String pendingKey = null;

            for (String line : lines) {
                line = line.trim();
                log.info("🔍 Line: {}", line);

                if (line.matches(".*표준[이하|이상|정도]?.*")) continue;

                // 1. pendingKey 우선 처리
                if (pendingKey != null && line.matches(".*\\d+.*")) {
                    String value = extractFirstDecimalOutsideParentheses(line);
                    if (value != null) {
                        result.putIfAbsent(keyNameMap.get(pendingKey), value);
                        pendingKey = null;
                        continue;
                    }
                }

                // 2. override 키 대응 (영문)
                for (Map.Entry<String, String> entry : overrideKeys.entrySet()) {
                    if (line.contains(entry.getKey()) && line.matches(".*\\d+.*")) {
                        String value = extractFirstDecimalOutsideParentheses(line);
                        if (value != null && !result.containsKey(entry.getValue())) {
                            result.put(entry.getValue(), value);
                        }
                    }
                }

                // 3. 한글 키 + 숫자 포함된 라인
                if (line.matches(".*[가-힣]+.*\\d+.*")) {
                    String rawKey = extractKey(line);
                    String normKey = normalizeKey(rawKey);

                    if (allowedKeys.contains(normKey)) {
                        String stdKey = keyNameMap.get(normKey);
                        String value = extractFirstDecimalOutsideParentheses(line);
                        if (value != null && !result.containsKey(stdKey)) {
                            result.put(stdKey, value);
                            continue;
                        }
                    }
                }

                // 4. 키만 있는 경우 → 다음 줄에서 값 받기
                if (line.matches(".*[가-힣]+.*") && !line.matches(".*\\d+.*")) {
                    String rawKey = extractKey(line);
                    String normKey = normalizeKey(rawKey);

                    if (allowedKeys.contains(normKey)) {
                        pendingKey = normKey;
                    }
                }
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException("OCR JSON 파싱 실패", e);
        }

        log.info("추출된 키-값 : {}", result);
        return result;
    }

    @Override
    public void saveOcrResult(Long userId, Map<String, String> ocrResult) {
        UserHealthData entity = new UserHealthData(userId, ocrResult);
        userHealthDataRepository.save(entity);
    }


    private String extractKey(String line) {
        Matcher matcher = Pattern.compile("([가-힣A-Za-z·\\s\\(\\)]+)").matcher(line);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String normalizeKey(String key) {
        if (key == null) return null;
        return key.replaceAll("\\s*\\(.*?\\)", "")  // 괄호 제거
                .trim()
                .replace(" ", "");
    }


    private String extractFirstDecimalOutsideParentheses(String line) {
        String cleaned = line.replaceAll("\\([^\\)]*\\)", " ");
        Matcher matcher = Pattern.compile("\\d+\\.\\d+").matcher(cleaned);
        return matcher.find() ? matcher.group() : null;
    }
}
