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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private final UserHealthDataRepository userHealthDataRepository;
    private final ClovaOcrClient clovaOcrClient;
    private final ObjectMapper objectMapper;

    // 🔥 핵심 개선: 범용적인 건강 지표 패턴들
    private static final Map<Pattern, String> HEALTH_PATTERNS = new LinkedHashMap<>();
    
    static {
        // 체중 관련 패턴들 (다양한 형태 지원)
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(체중|weight|몸무게|Weight)\\s*(?:\\([^)]*\\))?\\s*(\\d{2,3}(?:\\.\\d{1,2})?)\\s*(?:kg|킬로|키로)?"), "체중");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(골격근량|근육량|skeletal muscle|muscle mass|Skeletal Muscle Mass)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:kg|킬로)?"), "골격근량");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(체지방량|지방량|body fat mass|fat mass|Body FatMass)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:kg|킬로)?"), "체지방량");
        
        // 체성분 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(체수분|수분량|body water|total.*water|Total Body Water)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:L|리터|ℓ)?"), "체수분");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(단백질|protein|Protein)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:kg|킬로)?"), "단백질");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(무기질|미네랄|mineral|minerals|Minerals)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:kg|킬로)?"), "무기질");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(체지방|body fat|지방|fat)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:%|퍼센트|percent)?"), "체지방률");
        
        // BMI 및 기타 지수
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(BMI|body mass index|비만지수)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)"), "BMI");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(기초대사율|bmr|basal metabolic|기초대사|BMR)\\s*(?:\\([^)]*\\))?\\s*(\\d{3,4})\\s*(?:kcal|칼로리)?"), "기초대사율");
        
        // 혈압 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(수축기|최고혈압|systolic)\\s*(?:\\([^)]*\\))?\\s*(\\d{2,3})\\s*(?:mmHg)?"), "수축기혈압");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(이완기|최저혈압|diastolic)\\s*(?:\\([^)]*\\))?\\s*(\\d{2,3})\\s*(?:mmHg)?"), "이완기혈압");
        
        // 혈당 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(혈당|glucose|blood sugar|글루코스)\\s*(?:\\([^)]*\\))?\\s*(\\d{2,3})\\s*(?:mg/dL|mg/dl)?"), "혈당");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(당화혈색소|HbA1c|hba1c)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:%)?"), "당화혈색소");
        
        // 콜레스테롤 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(총콜레스테롤|total cholesterol|콜레스테롤)\\s*(?:\\([^)]*\\))?\\s*(\\d{2,3})\\s*(?:mg/dL|mg/dl)?"), "총콜레스테롤");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(HDL|good cholesterol|hdl)\\s*(?:\\([^)]*\\))?\\s*(\\d{2,3})\\s*(?:mg/dL|mg/dl)?"), "HDL콜레스테롤");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(LDL|bad cholesterol|ldl)\\s*(?:\\([^)]*\\))?\\s*(\\d{2,3})\\s*(?:mg/dL|mg/dl)?"), "LDL콜레스테롤");
        
        // 간기능 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(ALT|SGPT|alt)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3})\\s*(?:U/L|IU/L)?"), "ALT");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(AST|SGOT|ast)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3})\\s*(?:U/L|IU/L)?"), "AST");
        
        // 신장기능 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(크레아티닌|creatinine)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:mg/dL|mg/dl)?"), "크레아티닌");
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(요소질소|BUN|urea nitrogen)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3})\\s*(?:mg/dL|mg/dl)?"), "요소질소");
        
        // 염증 지표
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(CRP|c-reactive protein|염증수치)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:mg/L|mg/dl)?"), "CRP");
        
        // 갑상선 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(TSH|갑상선자극호르몬)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:mIU/L)?"), "TSH");
        
        // 비타민 관련
        HEALTH_PATTERNS.put(Pattern.compile("(?i)(비타민D|vitamin d|25-OH)\\s*(?:\\([^)]*\\))?\\s*(\\d{1,3}(?:\\.\\d{1,2})?)\\s*(?:ng/mL|ng/ml)?"), "비타민D");
        
        // 💡 범용 숫자-텍스트 패턴 (위의 특정 패턴에 매칭되지 않는 경우 사용)
        HEALTH_PATTERNS.put(Pattern.compile("([가-힣A-Za-z][가-힣A-Za-z\\s]{1,15})\\s*(?:\\([^)]*\\))?\\s*(\\d{1,4}(?:\\.\\d{1,3})?)\\s*(?:[가-힣A-Za-z/%]{0,10})?"), "기타지표");
    }

    // 유효성 검사를 위한 범위 정의 (더 유연하게)
    private static final Map<String, double[]> VALUE_RANGES = new HashMap<String, double[]>() {{
        put("체중", new double[]{20.0, 300.0});
        put("골격근량", new double[]{5.0, 100.0});
        put("체지방량", new double[]{0.0, 150.0});
        put("체수분", new double[]{15.0, 100.0});
        put("단백질", new double[]{3.0, 50.0});
        put("무기질", new double[]{1.0, 15.0});
        put("체지방률", new double[]{0.0, 70.0});
        put("BMI", new double[]{10.0, 60.0});
        put("기초대사율", new double[]{800.0, 4000.0});
        put("수축기혈압", new double[]{60.0, 300.0});
        put("이완기혈압", new double[]{30.0, 200.0});
        put("혈당", new double[]{30.0, 600.0});
        put("총콜레스테롤", new double[]{50.0, 500.0});
        put("HDL콜레스테롤", new double[]{10.0, 150.0});
        put("LDL콜레스테롤", new double[]{20.0, 400.0});
    }};

    public OcrServiceImpl(UserHealthDataRepository userHealthDataRepository, 
                         ClovaOcrClient clovaOcrClient, ObjectMapper objectMapper) {
        this.userHealthDataRepository = userHealthDataRepository;
        this.clovaOcrClient = clovaOcrClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, String> extractText(MultipartFile imageFile) {
        String ocrJson = clovaOcrClient.callClovaOcr(imageFile);
        Map<String, String> result = new LinkedHashMap<>();

        try {
            JsonNode fields = objectMapper
                    .readTree(ocrJson)
                    .path("images").get(0)
                    .path("fields");

            if (fields.size() == 0) return result;

            String rawText = fields.get(0).path("inferText").asText();
            log.info("🔍 원본 OCR 텍스트:\n{}", rawText);
            
            // 🔥 개선된 범용 파싱 로직
            result = parseHealthDataUniversally(rawText);
            
            // 데이터 검증 및 정제
            result = validateAndCleanResults(result);

        } catch (JsonProcessingException e) {
            log.error("❌ OCR JSON 파싱 실패", e);
            throw new RuntimeException("OCR JSON 파싱 실패", e);
        }

        log.info("✅ 최종 추출 결과: {}", result);
        return result;
    }

    // 🔥 핵심 개선: 범용적인 건강 데이터 파싱
    private Map<String, String> parseHealthDataUniversally(String rawText) {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> usedValues = new HashSet<>(); // 중복 값 방지
        
        log.info("🔍 범용 건강 데이터 파싱 시작");
        
        // 텍스트 정제
        String cleanedText = preprocessText(rawText);
        
        // 각 패턴에 대해 매칭 시도
        for (Map.Entry<Pattern, String> entry : HEALTH_PATTERNS.entrySet()) {
            Pattern pattern = entry.getKey();
            String categoryPrefix = entry.getValue();
            
            Matcher matcher = pattern.matcher(cleanedText);
            
            while (matcher.find()) {
                String keyMatch = matcher.group(1); // 키워드 부분
                String valueMatch = matcher.group(2); // 숫자 부분
                
                // 중복 값 체크
                if (usedValues.contains(valueMatch)) {
                    continue;
                }
                
                // 유효성 검사
                if (isValidHealthValue(categoryPrefix, keyMatch, valueMatch)) {
                    String finalKey;
                    
                    if ("기타지표".equals(categoryPrefix)) {
                        // 기타 지표의 경우 원본 키워드 사용
                        finalKey = normalizeKeyName(keyMatch);
                    } else {
                        // 정의된 카테고리의 경우 표준화된 이름 사용
                        finalKey = categoryPrefix;
                    }
                    
                    result.putIfAbsent(finalKey, valueMatch);
                    usedValues.add(valueMatch);
                    
                    log.info("✅ 패턴 매칭 성공: {} = {} (패턴: {})", finalKey, valueMatch, categoryPrefix);
                    
                    break; // 같은 패턴에서 첫 번째 매칭만 사용
                }
            }
        }
        
        log.info("🔍 파싱 완료, 추출된 항목 수: {}", result.size());
        return result;
    }

    // 텍스트 전처리
    private String preprocessText(String rawText) {
        return rawText
            .replaceAll("(?i)inbody|검사|결과|report", "") // 브랜드명/불필요한 단어 제거
            .replaceAll("표준\\s*[이하|이상|정도|범위]", "") // 표준 관련 텍스트 제거
            .replaceAll("권장\\s*[범위|수치]", "") // 권장 관련 텍스트 제거
            .replaceAll("정상\\s*[범위|수치]", "") // 정상 관련 텍스트 제거
            .replaceAll("\\s+", " ") // 다중 공백 정리
            .trim();
    }

    // 키 이름 정규화
    private String normalizeKeyName(String rawKey) {
        return rawKey.trim()
            .replaceAll("\\s+", "_") // 공백을 언더스코어로
            .replaceAll("[^가-힣A-Za-z0-9_]", "") // 특수문자 제거
            .toLowerCase();
    }

    // 건강 수치 유효성 검사 (더 유연하게)
    private boolean isValidHealthValue(String category, String key, String value) {
        try {
            double val = Double.parseDouble(value);
            
            // 범위가 정의된 경우 범위 체크
            double[] range = VALUE_RANGES.get(category);
            if (range != null) {
                boolean valid = val >= range[0] && val <= range[1];
                if (!valid) {
                    log.debug("⚠️ 범위 벗어남: {} = {} (범위: {}-{})", key, val, range[0], range[1]);
                    return false;
                }
            } else {
                // 범위가 정의되지 않은 경우 기본 검사
                if (val <= 0 || val > 10000) {
                    log.debug("⚠️ 기본 범위 벗어남: {} = {}", key, val);
                    return false;
                }
            }
            
            // 소수점 자릿수 체크 (너무 많은 소수점은 오인식일 가능성)
            String[] parts = value.split("\\.");
            if (parts.length > 1 && parts[1].length() > 3) {
                log.debug("⚠️ 소수점 자릿수 초과: {} = {}", key, value);
                return false;
            }
            
            return true;
            
        } catch (NumberFormatException e) {
            log.debug("⚠️ 숫자 형식 오류: {} = {}", key, value);
            return false;
        }
    }

    // 결과 검증 및 정제
    private Map<String, String> validateAndCleanResults(Map<String, String> result) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        
        for (Map.Entry<String, String> entry : result.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().trim().replaceAll("[^\\d\\.]", "");
            
            // 빈 값 체크
            if (value.isEmpty()) {
                log.warn("⚠️ 빈 값 제외: {}", key);
                continue;
            }
            
            // 최종 유효성 재검사
            try {
                double val = Double.parseDouble(value);
                if (val > 0 && val <= 10000) { // 기본 범위
                    cleaned.put(key, value);
                    log.debug("✅ 최종 검증 통과: {} = {}", key, value);
                } else {
                    log.warn("⚠️ 최종 검증 실패: {} = {}", key, value);
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ 최종 숫자 변환 실패: {} = {}", key, value);
            }
        }
        
        return cleaned;
    }

    @Override
    public void saveOcrResult(Long userId, Map<String, String> ocrResult) {
        UserHealthData entity = new UserHealthData(userId, ocrResult);
        userHealthDataRepository.save(entity);
        
        log.info("💾 사용자 {}의 건강 데이터 저장 완료: {} 항목", userId, ocrResult.size());
    }
}
