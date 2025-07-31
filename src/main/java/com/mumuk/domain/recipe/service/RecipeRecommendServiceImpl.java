package com.mumuk.domain.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumuk.domain.allergy.dto.response.AllergyResponse;
import com.mumuk.domain.allergy.service.AllergyService;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.client.OpenAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.reactive.function.client.WebClient;
// import com.mumuk.domain.recipe.converter.RecipeRecommendConverter; // 삭제
import com.mumuk.domain.recipe.converter.RecipeConverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collections;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
public class RecipeRecommendServiceImpl implements RecipeRecommendService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final IngredientService ingredientService;
    private final AllergyService allergyService;
    private final RecipeRepository recipeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration RECIPE_CACHE_TTL = Duration.ofDays(7); // 7일 동안 캐시
    private static final int BATCH_SIZE = 20; // 배치 처리 크기
    private static final int PAGE_SIZE = 100; // 페이징 크기
    private static final int MAX_RECOMMENDATIONS = 10; // 최대 추천 개수

    public RecipeRecommendServiceImpl(OpenAiClient openAiClient, ObjectMapper objectMapper,
                                   UserRepository userRepository, IngredientService ingredientService,
                                   AllergyService allergyService, RecipeRepository recipeRepository,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.ingredientService = ingredientService;
        this.allergyService = allergyService;
        this.recipeRepository = recipeRepository;
        this.redisTemplate = redisTemplate;
    }

    // 1. recommendAndSaveRecipes는 전체 흐름만 담당하도록 정리
    @Override
    public List<RecipeResponse.DetailRes> recommendAndSaveRecipesByIngredient(Long userId) {
        User user = getUser(userId);
        List<String> availableIngredients = getUserIngredients(userId);
        String redisKey = generateRedisKey(userId, availableIngredients);
        List<RecipeResponse.DetailRes> cachedResult = getCachedRecommendations(redisKey);
        if (cachedResult != null) return cachedResult;
        String prompt = createRecommendationPrompt(availableIngredients, user);
        List<Recipe> savedRecipes = callAIAndSaveRecipes(prompt);
        List<RecipeResponse.DetailRes> result = savedRecipes.stream()
            .map(this::toDetailRes)
                .collect(Collectors.toList());
        if (!result.isEmpty()) cacheRecommendations(redisKey, result);
        return result;
    }

    @Override
    public List<RecipeResponse.DetailRes> recommendByIngredient(Long userId) {
        User user = getUser(userId);
        List<String> availableIngredients = getUserIngredients(userId);
        String prompt = createRecommendationPrompt(availableIngredients, user);
        List<Recipe> savedRecipes = callAIAndSaveRecipes(prompt);
        List<RecipeResponse.DetailRes> result = savedRecipes.stream()
            .map(this::toDetailRes)
            .collect(Collectors.toList());
        return result.size() > 4 ? result.subList(0, 4) : result;
    }

    @Override
    public List<RecipeResponse.DetailRes> recommendRandom() {
        String prompt = buildRandomPrompt();
        List<Recipe> savedRecipes = callAIAndSaveRecipes(prompt);
        List<RecipeResponse.DetailRes> result = savedRecipes.stream()
            .map(this::toDetailRes)
            .collect(Collectors.toList());
        return result.size() > 4 ? result.subList(0, 4) : result;
    }

    @Override
    public List<RecipeResponse.SimpleRes> recommendRecipesByIngredient(Long userId) {
        User user = getUser(userId);
        List<String> availableIngredients = getUserIngredients(userId);
        List<String> allergyTypes = getUserAllergies(userId);

        // 페이징 처리로 성능 최적화
        PageRequest pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<Recipe> recipePage = recipeRepository.findAll(pageRequest);
        List<Recipe> allRecipes = recipePage.getContent();

        if (allRecipes.isEmpty()) {
            log.warn("DB에 레시피가 없습니다.");
            return new ArrayList<>();
        }

        log.info("처리할 레시피 수: {}", allRecipes.size());

        // AI가 각 레시피의 적합도를 평가 (배치 처리로 최적화됨)
        List<RecipeWithScore> recipesWithScores = evaluateRecipeSuitabilityByIngredient(
            allRecipes, availableIngredients, allergyTypes);

        // 적합도 점수로 내림차순 정렬 (높은 점수가 위로)
        recipesWithScores.sort((a, b) -> Double.compare(b.score, a.score));

        // SimpleRes로 변환하여 반환 (상위 10개만)
        return recipesWithScores.stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(recipeWithScore -> new RecipeResponse.SimpleRes(
                    recipeWithScore.recipe.getId(),
                    recipeWithScore.recipe.getTitle(),
                    recipeWithScore.recipe.getRecipeImage()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipeResponse.SimpleRes> recommendRecipesByHealth(Long userId) {
        User user = getUser(userId);
        List<String> availableIngredients = getUserIngredients(userId);
        List<String> allergyTypes = getUserAllergies(userId);
        String healthInfo = getUserHealthInfo(userId);
        
        // 페이징 처리로 성능 최적화
        PageRequest pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<Recipe> recipePage = recipeRepository.findAll(pageRequest);
        List<Recipe> allRecipes = recipePage.getContent();
        
        if (allRecipes.isEmpty()) {
            log.warn("DB에 레시피가 없습니다.");
            return new ArrayList<>();
        }
        
        log.info("처리할 레시피 수: {}", allRecipes.size());
        
        // AI가 각 레시피의 적합도를 평가
        List<RecipeWithScore> recipesWithScores = evaluateRecipeSuitabilityByHealth(
            allRecipes, availableIngredients, allergyTypes, healthInfo);
        
        // 적합도 점수로 내림차순 정렬 (높은 점수가 위로)
        recipesWithScores.sort((a, b) -> Double.compare(b.score, a.score));
        
        // SimpleRes로 변환하여 반환 (상위 10개만)
        return recipesWithScores.stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(recipeWithScore -> new RecipeResponse.SimpleRes(
                    recipeWithScore.recipe.getId(),
                    recipeWithScore.recipe.getTitle(),
                    recipeWithScore.recipe.getRecipeImage()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipeResponse.SimpleRes> recommendRecipesByCategories(String categories) {
        try {
            String[] categoryArray = categories.split(",");
            List<RecipeCategory> recipeCategories = new ArrayList<>();
            
            for (String category : categoryArray) {
                try {
                    RecipeCategory recipeCategory = RecipeCategory.valueOf(category.trim().toUpperCase());
                    recipeCategories.add(recipeCategory);
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 카테고리: {}", category.trim());
                }
            }
            
            if (recipeCategories.isEmpty()) {
                log.warn("유효한 카테고리가 없습니다: {}", categories);
                return new ArrayList<>();
            }
            
            List<Recipe> recipes = recipeRepository.findByCategoriesIn(recipeCategories);
            
            return recipes.stream()
                    .limit(MAX_RECOMMENDATIONS) // 상위 10개만 반환
                    .map(recipe -> new RecipeResponse.SimpleRes(
                        recipe.getId(),
                        recipe.getTitle(),
                        recipe.getRecipeImage()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("카테고리 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<RecipeResponse.SimpleRes> recommendRandomRecipes() {
        // 페이징 처리로 성능 최적화
        PageRequest pageRequest = PageRequest.of(0, PAGE_SIZE);
        Page<Recipe> recipePage = recipeRepository.findAll(pageRequest);
        List<Recipe> allRecipes = new ArrayList<>(recipePage.getContent()); // 새로운 ArrayList로 복사
        
        if (allRecipes.isEmpty()) {
            log.warn("DB에 레시피가 없습니다.");
            return new ArrayList<>();
        }
        
        log.info("처리할 레시피 수: {}", allRecipes.size());
        
        // 랜덤하게 섞기
        Collections.shuffle(allRecipes);
        
        return allRecipes.stream()
                .limit(MAX_RECOMMENDATIONS) // 상위 10개만 반환
                .map(recipe -> new RecipeResponse.SimpleRes(
                    recipe.getId(),
                    recipe.getTitle(),
                    recipe.getRecipeImage()
                ))
                .collect(Collectors.toList());
    }

    // 레시피와 점수를 함께 저장하는 내부 클래스
    private static class RecipeWithScore {
        Recipe recipe;
        double score;
        
        RecipeWithScore(Recipe recipe, double score) {
            this.recipe = recipe;
            this.score = score;
        }
    }

    /**
     * 재료 기반 레시피 적합도 평가 (배치 처리)
     */
    private List<RecipeWithScore> evaluateRecipeSuitabilityByIngredient(List<Recipe> recipes, 
                                                                       List<String> availableIngredients, 
                                                                       List<String> allergyTypes) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();
        
        // 재료 목록을 5개로 제한
        List<String> limitedIngredients = availableIngredients.size() > 5 
            ? availableIngredients.subList(0, 5) 
            : availableIngredients;
        
        log.info("사용자 보유 재료: {}", String.join(", ", limitedIngredients));
        
        // 배치 크기 제한 (성능 최적화)
        for (int i = 0; i < recipes.size(); i += BATCH_SIZE) {
            List<Recipe> batch = recipes.subList(i, Math.min(i + BATCH_SIZE, recipes.size()));
            try {
                // 배치 처리
                recipesWithScores.addAll(processBatch(batch, limitedIngredients, allergyTypes));
            } catch (Exception e) {
                log.warn("배치 처리 실패, 개별 처리로 전환: {}", e.getMessage());
                recipesWithScores.addAll(processIndividual(batch, limitedIngredients, allergyTypes));
            }
        }
        
        return recipesWithScores;
    }

    /**
     * 배치 처리 메서드
     */
    private List<RecipeWithScore> processBatch(List<Recipe> recipes, 
                                             List<String> availableIngredients, 
                                             List<String> allergyTypes) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();
        
        try {
            // 배치 처리: 모든 레시피를 한 번에 AI에게 전달
            String batchPrompt = createBatchIngredientSuitabilityPrompt(recipes, availableIngredients, allergyTypes);
            log.info("배치 프롬프트 생성 완료");
            
            String batchResponse = callAI(batchPrompt);
            log.info("AI 배치 응답: {}", batchResponse);
            
            // AI 응답에서 각 레시피의 점수 파싱
            Map<String, Double> scores = parseBatchScores(batchResponse, recipes);
            log.info("파싱된 점수: {}", scores);
            
            for (Recipe recipe : recipes) {
                double score = scores.getOrDefault(recipe.getTitle(), 5.0);
                log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);
                
                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            }
            
        } catch (Exception e) {
            log.warn("배치 적합도 평가 실패: {}", e.getMessage());
            throw e; // 상위에서 개별 처리로 전환하도록 예외 재발생
        }
        
        return recipesWithScores;
    }

    /**
     * 개별 처리 메서드
     */
    private List<RecipeWithScore> processIndividual(List<Recipe> recipes, 
                                                  List<String> availableIngredients, 
                                                  List<String> allergyTypes) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();
        
        for (Recipe recipe : recipes) {
            try {
                log.info("레시피 '{}' 재료: {}", recipe.getTitle(), recipe.getIngredients());
                
                String prompt = createIngredientSuitabilityPrompt(recipe, availableIngredients, allergyTypes);
                double score = callAIForSuitabilityScore(prompt);
                
                log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);
                
                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            } catch (Exception e) {
                log.warn("레시피 {} 적합도 평가 실패: {}", recipe.getTitle(), e.getMessage());
                recipesWithScores.add(new RecipeWithScore(recipe, 5.0));
            }
        }
        
        return recipesWithScores;
    }

    /**
     * 건강 정보 기반 레시피 적합도 평가
     */
    private List<RecipeWithScore> evaluateRecipeSuitabilityByHealth(List<Recipe> recipes, 
                                                                   List<String> availableIngredients, 
                                                                   List<String> allergyTypes, 
                                                                   String healthInfo) {
        List<RecipeWithScore> recipesWithScores = new ArrayList<>();
        
        // 재료 목록을 5개로 제한
        List<String> limitedIngredients = availableIngredients.size() > 5 
            ? availableIngredients.subList(0, 5) 
            : availableIngredients;
        
        log.info("사용자 보유 재료: {}", String.join(", ", limitedIngredients));
        log.info("사용자 건강 정보: {}", healthInfo);
        
        for (Recipe recipe : recipes) {
            try {
                log.info("레시피 '{}' 재료: {}", recipe.getTitle(), recipe.getIngredients());
                
                String prompt = createHealthSuitabilityPrompt(recipe, limitedIngredients, allergyTypes, healthInfo);
                double score = callAIForSuitabilityScore(prompt);
                
                log.info("레시피 '{}' 적합도 점수: {}", recipe.getTitle(), score);
                
                // AI가 0점을 주면 알레르기 충돌로 간주하여 제외
                if (score > 0) {
                    recipesWithScores.add(new RecipeWithScore(recipe, score));
                } else {
                    log.info("레시피 {} 제외됨 (AI가 알레르기 충돌로 판단)", recipe.getTitle());
                }
            } catch (Exception e) {
                log.warn("레시피 {} 적합도 평가 실패: {}", recipe.getTitle(), e.getMessage());
                // 평가 실패 시 기본 점수 5.0 부여
                recipesWithScores.add(new RecipeWithScore(recipe, 5.0));
            }
        }
        
        return recipesWithScores;
    }

    /**
     * 재료 기반 적합도 평가 프롬프트 생성
     */
    private String createIngredientSuitabilityPrompt(Recipe recipe, 
                                                   List<String> availableIngredients, 
                                                   List<String> allergyTypes) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("사용자가 가지고 있는 재료: ").append(String.join(", ", availableIngredients)).append("\n")
            .append("레시피 제목: ").append(recipe.getTitle()).append("\n")
            .append("레시피 재료: ").append(recipe.getIngredients()).append("\n\n")
            .append("위 정보를 바탕으로 다음 기준으로 적합도를 평가해주세요:\n")
            .append("1. 사용자 재료와 레시피 재료의 일치도 (주요 재료가 일치하면 높은 점수)\n")
            .append("2. 대체 가능한 재료 고려 (예: 삼겹살↔목살, 고등어↔생선, 백합↔조개류)\n")
            .append("3. 알레르기 성분이 포함되어 있으면 0점\n")
            .append("4. 사용자 재료로 만들 수 있는 정도 (재료가 많을수록 높은 점수)\n\n")
            .append("점수 기준:\n")
            .append("- 9-10점: 사용자 재료로 완벽하게 만들 수 있음\n")
            .append("- 7-8점: 주요 재료가 일치하고 대체 가능\n")
            .append("- 5-6점: 일부 재료만 일치하지만 가능함\n")
            .append("- 3-4점: 재료가 부족하지만 기본 재료는 있음\n")
            .append("- 1-2점: 거의 재료가 없음\n")
            .append("- 0점: 알레르기 성분 포함\n\n")
            .append("적합도 점수만 숫자로 응답해주세요 (예: 8.5)");
        
        return promptBuilder.toString();
    }

    /**
     * 건강 정보 기반 적합도 평가 프롬프트 생성
     */
    private String createHealthSuitabilityPrompt(Recipe recipe, 
                                               List<String> availableIngredients, 
                                               List<String> allergyTypes, 
                                               String healthInfo) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("사용자가 가지고 있는 재료: ").append(String.join(", ", availableIngredients)).append("\n")
            .append("사용자 건강 정보: ").append(healthInfo).append("\n")
            .append("레시피 제목: ").append(recipe.getTitle()).append("\n")
            .append("레시피 재료: ").append(recipe.getIngredients()).append("\n")
            .append("레시피 영양정보 - 칼로리: ").append(recipe.getCalories()).append("kcal, 단백질: ").append(recipe.getProtein()).append("g, 탄수화물: ").append(recipe.getCarbohydrate()).append("g, 지방: ").append(recipe.getFat()).append("g\n\n")
            .append("위 정보를 바탕으로 다음 기준으로 적합도를 평가해주세요:\n")
            .append("1. 사용자 재료와 레시피 재료의 일치도\n")
            .append("2. 건강 정보에 따른 영양 적합성\n")
            .append("3. 대체 가능한 재료 고려\n")
            .append("4. 알레르기 성분이 포함되어 있으면 0점\n\n")
            .append("점수 기준:\n")
            .append("- 9-10점: 재료도 완벽하고 건강에도 좋음\n")
            .append("- 7-8점: 재료가 일치하고 건강에 적합함\n")
            .append("- 5-6점: 재료는 가능하고 건강상 보통\n")
            .append("- 3-4점: 재료가 부족하거나 건강상 부적합\n")
            .append("- 1-2점: 재료도 부족하고 건강상 부적합\n")
            .append("- 0점: 알레르기 성분 포함\n\n")
            .append("적합도 점수만 숫자로 응답해주세요 (예: 8.5)");
        
        return promptBuilder.toString();
    }

    /**
     * AI를 호출하여 적합도 점수를 받아오는 메서드
     */
    private double callAIForSuitabilityScore(String prompt) {
        try {
            String response = callAI(prompt);
            if (response == null || response.isEmpty()) {
                return 5.0; // 기본 점수
            }
            
            // AI 응답에서 점수 추출
            String scoreStr = response.trim();
            
            // JSON 응답인 경우 처리
            if (scoreStr.startsWith("{")) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(scoreStr);
                    String content = jsonNode.path("choices").path(0).path("message").path("content").asText();
                    if (!content.isEmpty()) {
                        scoreStr = content.trim();
                    }
                } catch (Exception e) {
                    log.warn("JSON 파싱 실패: {}", e.getMessage());
                    return 5.0;
                }
            }
            
            // 숫자만 추출 (소수점 포함)
            scoreStr = scoreStr.replaceAll("[^0-9.]", "");
            
            if (scoreStr.isEmpty()) {
                log.warn("점수 추출 실패, 기본값 5.0 사용");
                return 5.0;
            }
            
            double score = Double.parseDouble(scoreStr);
            
            // 점수 범위 검증 (0.0 ~ 10.0)
            if (score < 0.0 || score > 10.0) {
                log.warn("AI 응답의 점수가 범위를 벗어남: {}, 기본값 5.0 사용", score);
                return 5.0;
            }
            
            return score;
        } catch (Exception e) {
            log.warn("AI 적합도 평가 실패: {}, 기본값 5.0 사용", e.getMessage());
            return 5.0;
        }
    }

    private String generateRedisKey(Long userId, List<String> ingredients) {
        String ingredientsStr = String.join(",", ingredients);
        
        String key = "recipe:recommend:" + userId + ":" + ingredientsStr;
        
        if (key.length() > 200) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
                return "recipe:recommend:" + userId + ":" + bytesToHex(hash).substring(0, 16);
            } catch (Exception e) {
                return "recipe:recommend:" + userId + ":" + ingredientsStr.hashCode();
            }
        }
        
        return key;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private List<RecipeResponse.DetailRes> getCachedRecommendations(String redisKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                log.info("Redis 캐시에서 추천 결과 조회: {}", redisKey);
                return (List<RecipeResponse.DetailRes>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: {}", e.getMessage());
            // Redis 에러는 추천 기능을 중단시키지 않도록 BusinessException을 던지지 않음
        }
        return null;
    }

    private void cacheRecommendations(String redisKey, List<RecipeResponse.DetailRes> result) {
        try {
            redisTemplate.opsForValue().set(redisKey, result, RECIPE_CACHE_TTL);
            log.info("Redis에 추천 결과 캐싱: {}", redisKey);
        } catch (Exception e) {
            log.warn("Redis 캐싱 실패: {}", e.getMessage());
            // Redis 에러는 추천 기능을 중단시키지 않도록 BusinessException을 던지지 않음
        }
    }

    private String createRecommendationPrompt(List<String> availableIngredients, 
                                           User user) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 재료 목록을 5개로 제한 (성능과 품질의 균형)
        List<String> limitedIngredients = availableIngredients.size() > 5 
            ? availableIngredients.subList(0, 5) 
            : availableIngredients;
        
        promptBuilder.append("사용자가 보유한 식재료 목록을 기반으로, 실존하는 요리를 추천해줘.\n\n")
            .append("※ 요리 선택 기준:\n")
            .append("- 실제 존재하는 보편적인 요리만 추천 (억지 조합 금지)\n")
            .append("- 요리명은 검색으로 조리법을 찾을 수 있을 정도로 대중적이고 보편적\n")
            .append("- 예시: 된장찌개 O, 미나리 된장찌개 O, 돼지고기 앞다리살 감자 상추 된장찌개 X\n")
            .append("- 사용자 냉장고 재료를 기반으로 추천하되, 많이 들어가는 것보다 실제 보편적 레시피임이 중요\n\n")
            .append("※ 재료 포함 기준:\n")
            .append("- 요리에 필요한 모든 보편적인 재료를 포함 (선택사항은 제외)\n")
            .append("- 예시: 제육볶음 → 고추장, 설탕, 간장, 식용유, 앞다리살, 양파, 당근 (보편적)\n")
            .append("- 예시: 배추, 깻잎, 치킨스톡, 다시마, 로즈마리, 바질 등은 선택사항이므로 제외\n")
            .append("- 재료명은 최대한 한국어로 표기 (네기 X → 양파 O)\n")
            .append("- 기본 조미료: 소금, 후추, 식용유, 간장, 고추장, 설탕\n")
            .append("- 특수한 양념이나 허브는 필수적인 것만 포함 (로즈마리, 바질, 오레가노, 치킨스톡 등)\n\n")
            .append("※ 카테고리 선택:\n")
            .append("- 각 요리의 특성에 맞는 카테고리를 적절하게 선택, 여러개 선택 가능\n")
            .append("- 마땅히 없다면 OTHER 선택\n\n")
            .append("※ 추천 결과는 다음 JSON 형식으로 출력해줘:\n")
            .append("{\n")
            .append("  \"recommendations\": [\n")
            .append("    {\n")
            .append("      \"title\": \"레시피 제목\",\n")
            .append("      \"description\": \"레시피 설명\",\n")
            .append("      \"ingredients\": \"재료1, 재료2, 재료3\",\n")
            .append("      \"category\": \"BODY_WEIGHT_MANAGEMENT,HEALTH_MANAGEMENT\",\n")
            .append("      \"cookingTime\": 30,\n")
            .append("      \"calories\": 300,\n")
            .append("      \"protein\": 20,\n")
            .append("      \"carbohydrate\": 30,\n")
            .append("      \"fat\": 10\n")
            .append("    }\n")
            .append("  ]\n")
            .append("}\n\n")
            .append("※ 사용 가능한 카테고리: BODY_WEIGHT_MANAGEMENT, HEALTH_MANAGEMENT, WEIGHT_LOSS, MUSCLE_GAIN, SUGAR_REDUCTION, BLOOD_PRESSURE, CHOLESTEROL, DIGESTION, OTHER\n\n")
            .append("※ summary는 UI 상단에 한 줄로 보여줄 예정이므로 간결하고 매력적으로 작성해줘. 예: '바삭한 계란전으로 든든한 한끼 완성!'\n\n")
            .append("※ 사용자가 보유한 식재료 목록:\n")
            .append(String.join(", ", limitedIngredients)).append("\n\n")
            .append("위 재료들을 활용하여 만들 수 있는 보편적인 요리를 4가지 추천해줘.");

        String prompt = promptBuilder.toString();
        log.info("프롬프트 길이: {} characters", prompt.length());
        
        return prompt;
    }

    private List<Recipe> callAIAndSaveRecipes(String prompt) {
        try {
            String response = callAI(prompt);
            if (response == null || response.isEmpty()) {
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode choicesNode = root.path("choices");
            if (choicesNode.isMissingNode() || !choicesNode.isArray() || choicesNode.size() == 0) {
                throw new BusinessException(ErrorCode.OPENAI_NO_CHOICES);
            }
            
            String aiContent = choicesNode.get(0).path("message").path("content").asText();
            if (aiContent.isEmpty()) {
                throw new BusinessException(ErrorCode.OPENAI_MISSING_CONTENT);
            }
            
            String jsonContent = extractJsonFromResponse(aiContent);
            if (jsonContent.isEmpty()) {
                throw new BusinessException(ErrorCode.OPENAI_JSON_PARSE_ERROR);
            }
            
            JsonNode recommendationsRoot = objectMapper.readTree(jsonContent);
            JsonNode recommendationsNode = recommendationsRoot.path("recommendations");
            if (recommendationsNode.isMissingNode() || !recommendationsNode.isArray()) {
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }
            
            List<Recipe> nonDuplicateRecipes = new ArrayList<>();
            
            for (JsonNode rec : recommendationsNode) {
                try {
                    Recipe recipe = parseAndValidateRecipe(rec);
                    if (recipe != null && !isDuplicateRecipe(recipe)) {
                        nonDuplicateRecipes.add(recipe);
                    }
                } catch (Exception e) {
                    log.warn("레시피 파싱 실패: {}", e.getMessage());
                }
            }
            
            if (nonDuplicateRecipes.isEmpty()) {
                throw new BusinessException(ErrorCode.OPENAI_EMPTY_RECOMMENDATIONS);
            }
            
            // 중복되지 않는 레시피들을 DB에 저장
            List<Recipe> savedRecipes = saveNonDuplicateRecipes(nonDuplicateRecipes);
            
            return savedRecipes;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 추천 및 저장 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }

    /**
     * AI 응답에서 JSON 부분만 추출하는 메서드 (정규표현식 기반 간결화)
     */
    private String extractJsonFromResponse(String aiContent) {
        // ```json ... ``` 또는 ``` ... ``` 코드블록 추출
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = codeBlockPattern.matcher(aiContent);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 중괄호로 감싸진 JSON 추출
        int start = aiContent.indexOf("{");
        int end = aiContent.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return aiContent.substring(start, end + 1).trim();
        }
        // 그 외에는 전체 반환
        return aiContent.trim();
    }

    /**
     * AI 응답에서 레시피 정보를 파싱하고 검증하는 메서드
     */
    private Recipe parseAndValidateRecipe(JsonNode rec) {
        try {
            String title = rec.path("title").asText();
            String description = rec.path("description").asText();
            String category = rec.path("category").asText();
            String ingredients = parseIngredients(rec.path("ingredients"));
            
            // 필수 필드 검증
            if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || category.isEmpty()) {
                log.warn("필수 필드 누락 - title: {}, category: {}, ingredients: {}", title, category, ingredients);
                return null;
            }
            
            Recipe recipe = new Recipe();
            recipe.setTitle(title);
            recipe.setRecipeImage("default-recipe-image.jpg");
            recipe.setDescription(description);
            recipe.setCookingTime(rec.path("cookingTime").asLong(0));
            recipe.setCalories(rec.path("calories").asLong(0));
            recipe.setProtein(rec.path("protein").asLong(0));
            recipe.setCarbohydrate(rec.path("carbohydrate").asLong(0));
            recipe.setFat(rec.path("fat").asLong(0));
            recipe.setCategories(parseCategories(category));
            recipe.setIngredients(ingredients);
            
            return recipe;
        } catch (Exception e) {
            log.warn("레시피 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 레시피 중복 검사 (Redis → DB 순서)
     */
    private boolean isDuplicateRecipe(Recipe recipe) {
        String title = recipe.getTitle();
        String ingredients = recipe.getIngredients();
        
        // 1. Redis에서 중복 검사 (제목만 캐싱)
        try {
            String redisKey = "recipe:title:" + title.hashCode();
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                log.info("Redis에서 중복 레시피 발견: {}", title);
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 중복 검사 실패: {}", e.getMessage());
        }
        
        // 2. DB에서 중복 검사
        try {
            if (recipeRepository.existsByTitle(title)) {
                log.info("DB에서 중복 레시피 발견 (제목): {}", title);
                return true;
            }
            
            if (recipeRepository.existsByTitleAndIngredients(title, ingredients)) {
                log.info("DB에서 중복 레시피 발견 (제목+재료): {}", title);
                return true;
            }
        } catch (Exception e) {
            log.warn("DB 중복 검사 실패: {}", e.getMessage());
        }
        
        return false;
    }

    /**
     * 중복되지 않는 레시피들을 DB에 저장하고 Redis에 캐싱
     */
    private List<Recipe> saveNonDuplicateRecipes(List<Recipe> nonDuplicateRecipes) {
        List<Recipe> savedRecipes = new ArrayList<>();
        
        for (Recipe recipe : nonDuplicateRecipes) {
            try {
                Recipe savedRecipe = recipeRepository.save(recipe);
                savedRecipes.add(savedRecipe);
                log.info("레시피 저장 성공: {}", recipe.getTitle());
                
                // 저장 성공 시 Redis에 제목만 캐싱
                cacheRecipeTitleToRedis(savedRecipe);
                
            } catch (Exception e) {
                log.warn("레시피 저장 실패: {} - {}", recipe.getTitle(), e.getMessage());
            }
        }
        
        return savedRecipes;
    }

    /**
     * 레시피 제목을 Redis에 캐싱 (중복 방지용)
     */
    private void cacheRecipeTitleToRedis(Recipe recipe) {
        try {
            String redisKey = "recipe:title:" + recipe.getTitle().hashCode();
            redisTemplate.opsForValue().set(redisKey, recipe.getTitle(), RECIPE_CACHE_TTL);
            log.info("레시피 제목 Redis 캐싱 성공: {}", recipe.getTitle());
        } catch (Exception e) {
            log.warn("레시피 제목 Redis 캐싱 실패: {}", e.getMessage());
        }
    }

    /**
     * 재료 정보를 파싱하는 메서드
     */
    private String parseIngredients(JsonNode ingredientsNode) {
        if (ingredientsNode.isArray()) {
            List<String> ingredientsList = new ArrayList<>();
            for (JsonNode ingredient : ingredientsNode) {
                ingredientsList.add(ingredient.asText());
            }
            return String.join(", ", ingredientsList);
        } else {
            return ingredientsNode.asText();
        }
    }

    /**
     * 카테고리 정보를 파싱하는 메서드
     */
    private List<RecipeCategory> parseCategories(String category) {
        try {
            String[] categoryArray = category.split(",");
            List<RecipeCategory> recipeCategories = new ArrayList<>();
            
            for (String cat : categoryArray) {
                try {
                    RecipeCategory recipeCategory = RecipeCategory.valueOf(cat.trim());
                    recipeCategories.add(recipeCategory);
                } catch (IllegalArgumentException e) {
                    log.warn("알 수 없는 카테고리: {}", cat.trim());
                }
            }
            
            if (recipeCategories.isEmpty()) {
                log.warn("모든 카테고리가 유효하지 않음: {}, OTHER로 설정", category);
                recipeCategories.add(RecipeCategory.OTHER);
            }
            
            return recipeCategories;
        } catch (Exception e) {
            log.warn("카테고리 처리 실패: {}, OTHER로 설정", category);
            return Arrays.asList(RecipeCategory.OTHER);
        }
    }

    // callAIWithSmartModelSwitch 메서드 완전 대체 및 간결화
    private String callAI(String prompt) {
        String model = "gpt-4o-mini";
        String apiKey = System.getenv("OPEN_AI_KEY");
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("dummy") || apiKey.startsWith("test")) {
            log.error("API 키가 설정되지 않았습니다.");
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
        
        // API 키 길이 검증 (OpenAI API 키는 보통 51자)
        if (apiKey.length() < 20) {
            log.error("유효하지 않은 API 키 형식입니다.");
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
        
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            body.put("messages", messages);
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            if (response != null && !response.isEmpty()) {
                return response;
            }
        } catch (Exception e) {
            log.warn("gpt-4o-mini 모델로 AI 호출 실패: {}", e.getMessage());
        }
        throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
    }



    // 2. 역할별 private 메서드 분리 및 간결화
    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<String> getUserIngredients(Long userId) {
        List<IngredientResponse.RetrieveRes> ingredients = ingredientService.getAllIngredient(userId);
        if (ingredients.isEmpty()) {
            log.warn("사용자 {}의 재료 정보가 없습니다.", userId);
            throw new BusinessException(ErrorCode.RECIPE_EMPTY_INGREDIENTS);
        }
        return ingredients.stream()
            .map(IngredientResponse.RetrieveRes::getName)
            .collect(Collectors.toList());
    }

    private List<String> getUserAllergies(Long userId) {
        try {
            AllergyResponse.AllergyListRes allergyList = allergyService.getAllergyList(userId);
            return allergyList.getAllergyOptions().stream()
                .map(a -> a.getAllergyType().name())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("사용자 {}의 알레르기 정보 조회 실패: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private String getUserHealthInfo(Long userId) {
        try {
            User user = getUser(userId);
            // 실제 건강 정보 조회 로직 구현
            // TODO: HealthInfoService 구현 후 아래 주석 해제
            // return healthInfoService.getHealthInfo(userId);
            
            // 임시 구현: 사용자 정보에서 기본 건강 정보 추출
            StringBuilder healthInfo = new StringBuilder();
            healthInfo.append("사용자 건강 정보: ");
            
            // 사용자 기본 정보 활용
            if (user != null) {
                healthInfo.append("정상적인 건강 상태입니다. ");
                // TODO: 실제 건강 정보 테이블에서 조회
                // 예: 체중, 키, 건강 목표, 알레르기 등
            } else {
                healthInfo.append("사용자 정보를 찾을 수 없습니다. ");
            }
            
            healthInfo.append("특별한 건강 목표는 없습니다.");
            return healthInfo.toString();
            
        } catch (Exception e) {
            log.warn("사용자 {}의 건강 정보 조회 실패: {}", userId, e.getMessage());
            // TODO: BusinessException으로 변경
            // throw new BusinessException(ErrorCode.HEALTH_INFO_NOT_FOUND);
            return "사용자의 건강 정보를 불러올 수 없습니다.";
        }
    }

    private RecipeResponse.DetailRes toDetailRes(Recipe recipe) {
        return new RecipeResponse.DetailRes(
            recipe.getId(),
            recipe.getTitle(),
            recipe.getRecipeImage(),
            recipe.getDescription(),
            recipe.getCookingTime(),
            recipe.getCalories(),
            recipe.getProtein(),
            recipe.getCarbohydrate(),
            recipe.getFat(),
            recipe.getCategories().stream()
                .map(RecipeCategory::name)
                .collect(Collectors.toList()),
            recipe.getIngredients()
        );
    }

    private String buildRandomPrompt() {
        return "다양하고 맛있는 보편적인 요리를 추천해줘.\n\n" +
               "※ 요리 선택 기준:\n" +
               "- 실제 존재하는 보편적인 요리만 추천 (억지 조합 금지)\n" +
               "- 요리명은 검색으로 조리법을 찾을 수 있을 정도로 대중적이고 보편적\n" +
               "- 예시: 된장찌개 O, 미나리 된장찌개 O, 돼지고기 앞다리살 감자 상추 된장찌개 X\n" +
               "- 한국 요리, 중국 요리, 일본 요리, 서양 요리, 동남아 요리 등 다양한 문화권의 요리 포함\n" +
               "- 메인 요리, 반찬, 국물 요리, 볶음 요리, 구이 요리 등 다양한 조리법 포함\n" +
               "- 고기 요리, 생선 요리, 채식 요리, 면 요리 등 다양한 재료 활용\n\n" +
               "※ 재료 포함 기준:\n" +
               "- 요리에 필요한 모든 보편적인 재료를 포함 (선택사항은 제외)\n" +
               "- 예시: 제육볶음 → 고추장, 설탕, 간장, 식용유, 앞다리살, 양파, 당근 (보편적)\n" +
               "- 예시: 배추, 깻잎, 치킨스톡, 다시마, 로즈마리, 바질 등은 선택사항이므로 제외\n" +
               "- 재료명은 반드시 한국어로 표기 (네기 X → 양파 O, 대파 X → 파 O)\n" +
               "- 기본 조미료: 소금, 후추, 식용유, 간장, 고추장, 설탕, 마늘, 파, 양파, 당근\n" +
               "- 특수한 양념이나 허브는 제외 (로즈마리, 바질, 오레가노, 치킨스톡 등)\n\n" +
               "※ 카테고리 선택:\n" +
               "- 각 요리의 특성에 맞는 카테고리를 적절하게 선택, 여러개 선택 가능\n" +
               "- 마땅히 없다면 OTHER 선택\n\n" +
               "※ 추천 결과는 다음 JSON 형식으로 출력해줘:\n" +
               "{\n" +
               "  \"recommendations\": [\n" +
               "    {\n" +
               "      \"title\": \"레시피 제목\",\n" +
               "      \"description\": \"레시피 설명\",\n" +
               "      \"ingredients\": \"재료1, 재료2, 재료3\",\n" +
               "      \"category\": \"BODY_WEIGHT_MANAGEMENT,HEALTH_MANAGEMENT\",\n" +
               "      \"cookingTime\": 30,\n" +
               "      \"calories\": 300,\n" +
               "      \"protein\": 20,\n" +
               "      \"carbohydrate\": 30,\n" +
               "      \"fat\": 10\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               "※ 사용 가능한 카테고리: BODY_WEIGHT_MANAGEMENT, HEALTH_MANAGEMENT, WEIGHT_LOSS, MUSCLE_GAIN, SUGAR_REDUCTION, BLOOD_PRESSURE, CHOLESTEROL, DIGESTION, OTHER\n\n" +
               "※ summary는 UI 상단에 한 줄로 보여줄 예정이므로 간결하고 매력적으로 작성해줘. 예: '바삭한 계란전으로 든든한 한끼 완성!'\n\n" +
               "총 8개의 다양한 보편적인 요리를 추천해줘. 중복되지 않는 다양한 요리를 선택해주세요.";
    }

    /**
     * 배치 적합도 평가 프롬프트 생성
     */
    private String createBatchIngredientSuitabilityPrompt(List<Recipe> recipes, 
                                                        List<String> availableIngredients, 
                                                        List<String> allergyTypes) {
        // OpenAI 토큰 제한 고려 (대략 1토큰 = 4글자)
        int maxPromptLength = 12000; // 약 3000 토큰
        
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("사용자가 가지고 있는 재료: ").append(String.join(", ", availableIngredients)).append("\n\n")
            .append("다음 레시피들의 적합도를 평가해주세요:\n\n");
        
        int currentLength = promptBuilder.length();
        for (Recipe recipe : recipes) {
            String recipeText = "레시피: " + recipe.getTitle() + "\n" +
                               "재료: " + recipe.getIngredients() + "\n\n";
            
            if (currentLength + recipeText.length() > maxPromptLength) {
                log.warn("프롬프트 크기 제한 초과, {} 개 레시피만 처리", 
                        recipes.indexOf(recipe));
                break;
            }
            
            promptBuilder.append(recipeText);
            currentLength += recipeText.length();
        }
        
        promptBuilder.append("평가 기준:\n")
            .append("1. 사용자 재료와 레시피 재료의 일치도 (주요 재료가 일치하면 높은 점수)\n")
            .append("2. 대체 가능한 재료 고려 (예: 삼겹살↔목살, 고등어↔생선, 백합↔조개류)\n")
            .append("3. 알레르기 성분이 포함되어 있으면 0점\n")
            .append("4. 사용자 재료로 만들 수 있는 정도 (재료가 많을수록 높은 점수)\n\n")
            .append("점수 기준:\n")
            .append("- 9-10점: 사용자 재료로 완벽하게 만들 수 있음\n")
            .append("- 7-8점: 주요 재료가 일치하고 대체 가능\n")
            .append("- 5-6점: 일부 재료만 일치하지만 가능함\n")
            .append("- 3-4점: 재료가 부족하지만 기본 재료는 있음\n")
            .append("- 1-2점: 거의 재료가 없음\n")
            .append("- 0점: 알레르기 성분 포함\n\n")
            .append("반드시 다음 JSON 형태로만 응답해주세요:\n")
            .append("{\n")
            .append("  \"scores\": {\n");
        
        // 실제 처리된 레시피만 JSON에 포함
        int processedCount = 0;
        for (Recipe recipe : recipes) {
            if (processedCount >= Math.min(recipes.size(), 
                (maxPromptLength - currentLength) / 50)) { // 대략적인 레시피당 길이
                break;
            }
            promptBuilder.append("    \"").append(recipe.getTitle()).append("\": 점수");
            if (processedCount < Math.min(recipes.size() - 1, 
                (maxPromptLength - currentLength) / 50 - 1)) {
                promptBuilder.append(",");
            }
            promptBuilder.append("\n");
            processedCount++;
        }
        
        promptBuilder.append("  }\n")
            .append("}\n\n")
            .append("다른 설명이나 텍스트 없이 JSON만 응답해주세요.");
        
        return promptBuilder.toString();
    }

    /**
     * 배치 응답에서 점수 파싱
     */
    private Map<String, Double> parseBatchScores(String response, List<Recipe> recipes) {
        Map<String, Double> scores = new HashMap<>();
        
        log.info("배치 응답 전체: {}", response);
        
        try {
            // OpenAI API 표준 응답에서 content 추출
            JsonNode jsonNode = objectMapper.readTree(response);
            String aiContent = jsonNode.path("choices").path(0).path("message").path("content").asText();
            log.info("AI 응답 내용: {}", aiContent);
            
            // AI 응답에서 JSON 부분 추출
            String jsonContent = extractJsonFromResponse(aiContent);
            log.info("추출된 JSON 내용: {}", jsonContent);
            
            if (!jsonContent.isEmpty() && jsonContent.startsWith("{")) {
                JsonNode scoresJson = objectMapper.readTree(jsonContent);
                JsonNode scoresNode = scoresJson.path("scores");
                
                if (!scoresNode.isMissingNode()) {
                    log.info("scores 노드 내용: {}", scoresNode.toString());
                    
                    for (Recipe recipe : recipes) {
                        double score = scoresNode.path(recipe.getTitle()).asDouble(5.0);
                        scores.put(recipe.getTitle(), score);
                        log.info("레시피 '{}' 점수 파싱: {}", recipe.getTitle(), score);
                    }
                } else {
                    log.warn("AI 응답에 scores 노드가 없습니다.");
                    // 모든 레시피에 기본 점수 부여
                    for (Recipe recipe : recipes) {
                        scores.put(recipe.getTitle(), 5.0);
                    }
                }
            } else {
                log.warn("AI 응답에서 JSON을 추출할 수 없습니다.");
                // 모든 레시피에 기본 점수 부여
                for (Recipe recipe : recipes) {
                    scores.put(recipe.getTitle(), 5.0);
                }
            }
        } catch (Exception e) {
            log.warn("배치 점수 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 모든 레시피에 기본 점수 부여
            for (Recipe recipe : recipes) {
                scores.put(recipe.getTitle(), 5.0);
            }
        }
        
        log.info("최종 파싱 결과: {}", scores);
        return scores;
    }
} 