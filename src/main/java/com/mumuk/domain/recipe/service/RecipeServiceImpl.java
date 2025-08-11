package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.converter.RecipeConverter;
import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import java.time.Duration;
import com.mumuk.global.client.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

@Service
public class RecipeServiceImpl implements RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeServiceImpl.class);
    private static final Duration RECIPE_CACHE_TTL = Duration.ofDays(7); // 7일 동안 캐시
    private static final Duration AI_MATCH_CACHE_TTL = Duration.ofHours(6); // AI 매칭 결과 캐시 TTL

    private final RecipeRepository recipeRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final IngredientService ingredientService;

    public RecipeServiceImpl(RecipeRepository recipeRepository, UserRecipeRepository userRecipeRepository, RedisTemplate<String, Object> redisTemplate,
                           OpenAiClient openAiClient, ObjectMapper objectMapper,
                           IngredientService ingredientService) {
        this.recipeRepository = recipeRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.redisTemplate = redisTemplate;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.ingredientService = ingredientService;
    }

    @Override
    @Transactional
    public void createRecipe(RecipeRequest.CreateReq request) {
        log.info("레시피 등록 시작: {}", request.getTitle());
        
        // 1. Redis로 중복 검증 (빠른 검증)
        log.info("Redis 중복 검증 시작: {}", request.getTitle());
        if (isRecipeTitleExistsInRedis(request.getTitle())) {
            log.warn("Redis에서 중복 레시피 발견: {}", request.getTitle());
            throw new BusinessException(ErrorCode.RECIPE_DUPLICATE_TITLE);
        }
        log.info("Redis 중복 검증 통과: {}", request.getTitle());
        
        // 2. DB로 중복 검증 (정확한 검증)
        log.info("DB 중복 검증 시작: {}", request.getTitle());
        if (recipeRepository.existsByTitle(request.getTitle())) {
            log.warn("DB에서 중복 레시피 발견: {}", request.getTitle());
            throw new BusinessException(ErrorCode.RECIPE_DUPLICATE_TITLE);
        }
        log.info("DB 중복 검증 통과: {}", request.getTitle());
        
        // 3. DB에 레시피 저장
        Recipe recipe = RecipeConverter.toRecipe(request);
        Recipe savedRecipe = recipeRepository.save(recipe);
        log.info("DB 저장 완료: {} (ID: {})", savedRecipe.getTitle(), savedRecipe.getId());
        
        // 4. DB 저장 성공 시 Redis에 완전한 캐싱
        cacheRecipeTitleToRedis(savedRecipe);
        log.info("레시피 등록 완료: {}", savedRecipe.getTitle());
    }

    @Override
    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND);
        }
        recipeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse.DetailRes getRecipeDetail(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
        return RecipeConverter.toDetailRes(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findNamesByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        RecipeCategory recipeCategory;
        try {
            recipeCategory = RecipeCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        return recipeRepository.findNamesByCategories(List.of(recipeCategory));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findNamesByCategories(String categories) {
        if (categories == null || categories.isBlank()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        
        String[] categoryArray = categories.split(",");
        List<RecipeCategory> recipeCategories = new ArrayList<>();
        List<String> invalidCategories = new ArrayList<>();
        
        for (String category : categoryArray) {
            try {
                RecipeCategory recipeCategory = RecipeCategory.valueOf(category.trim().toUpperCase());
                recipeCategories.add(recipeCategory);
            } catch (IllegalArgumentException e) {
                invalidCategories.add(category.trim());
            }
        }
        
        if (!invalidCategories.isEmpty()) {
            log.warn("유효하지 않은 카테고리들이 무시되었습니다: {}", String.join(", ", invalidCategories));
        }
        
        if (recipeCategories.isEmpty()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        
        return recipeRepository.findNamesByCategories(recipeCategories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponse.DetailRes> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        return recipes.stream()
                .map(RecipeConverter::toDetailRes)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRecipeResponse.RecipeSummaryDTO> getRecipeSummaries(Long userId) {
        List<Recipe> recipes = recipeRepository.findAll();
        List<Long> recipeIds = recipes.stream()
                .map(Recipe::getId)
                .collect(Collectors.toList());
        Map<Long, Boolean> likedMap = getUserRecipeLikedMap(userId, recipeIds);
        return recipes.stream()
                .map(recipe -> RecipeConverter.toRecipeSummaryDTO(recipe, likedMap.get(recipe.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateRecipe(Long id, RecipeRequest.CreateReq request) {
        Recipe recipe = recipeRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));

        if (request.getTitle() != null) recipe.setTitle(request.getTitle());
        if (request.getRecipeImage() != null) recipe.setRecipeImage(request.getRecipeImage());
        if (request.getDescription() != null) recipe.setDescription(request.getDescription());
        if (request.getCookingTime() != null) recipe.setCookingTime(request.getCookingTime());
        if (request.getCalories() != null) recipe.setCalories(request.getCalories());
        if (request.getProtein() != null) recipe.setProtein(request.getProtein());
        if (request.getCarbohydrate() != null) recipe.setCarbohydrate(request.getCarbohydrate());
        if (request.getFat() != null) recipe.setFat(request.getFat());
        if (request.getIngredients() != null) recipe.setIngredients(request.getIngredients());

        if (request.getCategories() != null) {
            List<RecipeCategory> categoryList = new ArrayList<>();
            for (String categoryStr : request.getCategories()) {
                try {
                    categoryList.add(RecipeCategory.valueOf(categoryStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(ErrorCode.RECIPE_INVALID_CATEGORY);
                }
            }
            recipe.setCategories(categoryList);
        }

        recipeRepository.save(recipe);
    }

    /**
     * 레시피 제목을 Redis에 캐싱 (ZSet으로 통합)
     */
    private void cacheRecipeTitleToRedis(Recipe recipe) {
        try {
            log.debug("Redis 캐싱 시작: '{}'", recipe.getTitle());
            
            // ZSet 하나로 중복 방지 + 자동완성 모두 처리
            String titleToCache = recipe.getTitle().toLowerCase();
            log.debug("캐싱할 제목 (소문자): '{}'", titleToCache);
            
            Boolean result = redisTemplate.opsForZSet().add("recipetitles", titleToCache, 0);
            log.debug("Redis ZSet 추가 결과: '{}' -> 성공: {}", titleToCache, result);
            
            if (result != null && result) {
                log.info("레시피 제목 Redis 캐싱 성공: '{}' (ZSet 통합)", recipe.getTitle());
            } else {
                log.warn("레시피 제목 Redis 캐싱 실패 (이미 존재): '{}'", recipe.getTitle());
            }
        } catch (Exception e) {
            log.warn("레시피 제목 Redis 캐싱 실패: {} - {}", recipe.getTitle(), e.getMessage());
        }
    }

    /**
     * Redis ZSet에 해당 제목의 레시피가 존재하는지 확인합니다.
     */
    private boolean isRecipeTitleExistsInRedis(String title) {
        try {
            log.debug("Redis ZSet 검색 시작: '{}'", title);
            
            // ZSet에서 제목 검색 (소문자로 통일)
            String searchTitle = title.toLowerCase();
            log.debug("검색할 제목 (소문자): '{}'", searchTitle);
            
            Double score = redisTemplate.opsForZSet().score("recipetitles", searchTitle);
            boolean exists = score != null;
            
            log.debug("Redis 검색 결과: '{}' -> 존재: {}", searchTitle, exists);
            
            if (exists) {
                log.info("Redis에서 레시피 제목 발견: '{}' (점수: {})", searchTitle, score);
            } else {
                log.debug("Redis에서 레시피 제목 없음: '{}'", searchTitle);
            }
            
            return exists;
        } catch (Exception e) {
            log.warn("Redis 중복 검사 실패: {} - {}", title, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse.IngredientMatchingRes matchIngredientsByAI(Long userId, Long recipeId) {
        log.info("AI 기반 재료 매칭 시작: 사용자 ID: {}, 레시피 ID: {}", userId, recipeId);
        
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
        
        List<String> userIngredients = getUserIngredients(userId);
        List<String> recipeIngredients = parseIngredients(recipe.getIngredients());
        
        log.info("사용자 재료: {}", userIngredients);
        log.info("레시피 재료: {}", recipeIngredients);
        
        // 정규화된 재료 목록 생성 (소문자, trim, 정렬)
        List<String> normUser = userIngredients.stream()
                .filter(s -> s != null)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.toList());
        List<String> normRecipe = recipeIngredients.stream()
                .filter(s -> s != null)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.toList());
        
        // 사용자 재료 Set 생성 (replaceable 검증용)
        Set<String> normUserSet = new HashSet<>(normUser);
        
        // Redis 캐싱 키 생성 (정규화된 재료 해시 포함)
        String cacheKey = String.format("ai-match:%d:%d:%d:%d",
                userId, recipeId, normUser.hashCode(), normRecipe.hashCode());
        
        // 캐시된 결과 확인
        String cachedResult = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            log.info("캐시된 AI 분석 결과 사용");
            return parseAIAnalysis(recipe, cachedResult, normUserSet);
        }
        
        // AI 분석 요청
        String aiAnalysis = analyzeIngredientsWithAI(userIngredients, recipeIngredients);
        log.info("AI 분석 결과: {}", aiAnalysis);
        
                // 결과를 Redis에 캐싱 (fallback 결과는 캐시하지 않음)
        if (!isFallbackAIResult(aiAnalysis, recipeIngredients)) {
            redisTemplate.opsForValue().set(cacheKey, aiAnalysis, AI_MATCH_CACHE_TTL);
        } else {
            log.info("AI fallback 결과는 캐시하지 않습니다. key={}", cacheKey);
        }

        // AI 분석 결과 파싱
        RecipeResponse.IngredientMatchingRes result = parseAIAnalysis(recipe, aiAnalysis, normUserSet);
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse.IngredientMatchingRes matchIngredientsSimple(Long userId, Long recipeId) {
        log.info("단순 재료 매칭 시작: 사용자 ID: {}, 레시피 ID: {}", userId, recipeId);
        
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
        
        List<String> userIngredients = getUserIngredients(userId);
        List<String> recipeIngredients = parseIngredients(recipe.getIngredients());
        
        // 단순 매칭 수행
        RecipeResponse.IngredientMatchingRes result = performSimpleMatching(recipe, recipeIngredients, userIngredients);
        
        return result;
    }



    /**
     * 사용자의 냉장고 재료 목록을 조회합니다.
     */
    private List<String> getUserIngredients(Long userId) {
        try {
            // IngredientService를 통해 사용자의 재료 목록 조회
            List<IngredientResponse.RetrieveRes> ingredients = ingredientService.getAllIngredient(userId);
            return ingredients.stream()
                    .filter(Objects::nonNull)
                    .map(IngredientResponse.RetrieveRes::getName)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("사용자 재료 조회 실패: {} - {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 레시피 재료 문자열을 파싱하여 개별 재료 목록으로 변환합니다.
     */
    private List<String> parseIngredients(String ingredients) {
        if (ingredients == null || ingredients.isBlank()) {
            return new ArrayList<>();
        }
        
        // 쉼표, 줄바꿈, 세미콜론 등으로 구분된 재료들을 파싱
        return List.of(ingredients.split("[,;\n]+"))
                .stream()
                .map(String::trim)
                .filter(ingredient -> !ingredient.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * AI를 사용하여 재료 매칭을 분석합니다.
     */
    private String analyzeIngredientsWithAI(List<String> userIngredients, List<String> recipeIngredients) {
        try {
            String prompt = buildIngredientMatchingPrompt(userIngredients, recipeIngredients);
            String response = callAI(prompt);
            if (response == null || response.isBlank()) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("match", List.of());
                fallback.put("mismatch", recipeIngredients);
                fallback.put("replaceable", List.of());
                return objectMapper.writeValueAsString(fallback);
            }
            return response;
        } catch (Exception e) {
            log.error("AI 재료 매칭 분석 실패: {}", e.getMessage());
            // AI 실패 시 기본 JSON 형태로 반환
            try {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("match", List.of());
                fallback.put("mismatch", recipeIngredients);
                fallback.put("replaceable", List.of());
                return objectMapper.writeValueAsString(fallback);
            } catch (Exception jsonException) {
                log.error("JSON 생성 실패: {}", jsonException.getMessage());
                // 최후의 수단으로 간단한 JSON 문자열 반환
                return String.format("""
                    {
                        "match": [],
                        "mismatch": ["%s"],
                        "replaceable": []
                    }
                    """, String.join("\", \"", recipeIngredients));
            }
        }
    }

    /**
     * 재료 매칭을 위한 AI 프롬프트를 생성합니다.
     */
    private String buildIngredientMatchingPrompt(List<String> userIngredients, List<String> recipeIngredients) {
        return String.format("""
            사용자 재료: %s
            레시피 재료: %s
            
            각 레시피 재료를 다음 3가지로 분류해주세요:
            
            1. match: 사용자 재료와 정확히 일치하는 레시피 재료
            2. mismatch: 사용자 재료에 없고 대체도 불가능한 레시피 재료  
            3. replaceable: 사용자 재료로 대체 가능한 레시피 재료
            
            분류 로직:
            각 레시피 재료에 대해:
            1) 사용자 재료 목록에서 정확히 일치하는 것이 있으면 → match
            2) 정확히 일치하지 않지만 같은 종류의 사용자 재료가 있으면 → replaceable
            3) 그 외의 경우 → mismatch
            
                         대체 가능한 경우 (3가지 기준):
             
             1. 포함 관계 (상위 개념 ↔ 하위 개념):
             - 돼지고기 ↔ 앞다리살, 목살, 삼겹살 (돼지고기가 상위, 구체적 부위가 하위)
             - 소고기 ↔ 등심, 안심, 갈비 (소고기가 상위, 구체적 부위가 하위)
             - 닭고기 ↔ 닭가슴살, 닭다리, 닭날개 (닭고기가 상위, 구체적 부위가 하위)
             
             2. 같은 종류 + 같은 용도의 대체재:
             - 돼지고기(구이용) ↔ 삼겹살(구이용) (같은 돼지고기, 같은 구이용도)
             - 돼지고기(구이용) ↔ 목살(구이용) (같은 돼지고기, 같은 구이용도)
             - 돼지고기(탕용) ↔ 앞다리살(탕용) (같은 돼지고기, 같은 탕용도)
             - 마늘 ↔ 흑마늘 (같은 마늘이지만 다른 종류)
             - 상추 ↔ 로메인 (같은 상추 종류)
             - 양파 ↔ 대파 (같은 파 종류)
             - 간장 ↔ 진간장 (같은 간장 종류)
             
             3. 비슷한 역할의 조미료:
             - 설탕 ↔ 올리고당 (단맛 조미료)
             - 소금 ↔ 천일염 (염분 조미료)
            
                         대체 불가능한 경우:
             - 돼지 등심(돈까스용) ↔ 삼겹살(구이용) (같은 돼지고기지만 용도가 완전히 다름)
             - 돼지 등심(돈까스용) ↔ 목살(구이용) (같은 돼지고기지만 용도가 완전히 다름)
             - 돼지 등심(돈까스용) ↔ 앞다리살(탕용) (같은 돼지고기지만 용도가 완전히 다름)
             - 소고기 ↔ 돼지고기 (완전히 다른 고기)
             - 고기 ↔ 생선 (완전히 다른 단백질)
             - 채소 ↔ 고기 (완전히 다른 재료)
             - 조미료 ↔ 주재료 (완전히 다른 역할)
            
            절대 규칙:
            - 하나의 레시피 재료는 반드시 한 곳에만 분류
            - replaceable의 recipeIngredient는 반드시 레시피 재료 목록에 있어야 함
            - 사용자 재료 목록에 없는 재료는 절대 replaceable에 포함하지 마세요!
            
            JSON 형태로 응답:
            {
                "match": ["레시피재료1", "레시피재료2"],
                "mismatch": ["레시피재료3", "레시피재료4"],
                "replaceable": [
                    {
                        "recipeIngredient": "레시피재료",
                        "userIngredient": "사용자재료"
                    }
                ]
            }
            """, userIngredients, recipeIngredients);
    }

    /**
     * AI 응답을 호출합니다.
     */
    private String callAI(String prompt) {
        try {
            return openAiClient.chat(prompt).block(java.time.Duration.ofSeconds(15));
        } catch (Exception e) {
            log.error("AI 호출 실패", e); // 스택트레이스 포함 로깅
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }

    /**
     * AI가 실패해 생성한 fallback JSON인지 판별
     */
    private boolean isFallbackAIResult(String json, List<String> recipeIngredients) {
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean matchEmpty = root.has("match") && root.get("match").isArray() && root.get("match").isEmpty();
            boolean replEmpty = root.has("replaceable") && root.get("replaceable").isArray() && root.get("replaceable").isEmpty();
            if (!(matchEmpty && replEmpty) || !root.has("mismatch") || !root.get("mismatch").isArray()) return false;
            // mismatch가 레시피 재료와 동일한지(순서 무관) 확인
            Set<String> mis = new HashSet<>();
            root.get("mismatch").forEach(n -> { if (n.isTextual()) mis.add(n.asText()); });
            Set<String> ri = recipeIngredients.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            return mis.equals(ri);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * AI 분석 결과를 파싱합니다.
     */
    private RecipeResponse.IngredientMatchingRes parseAIAnalysis(Recipe recipe, String aiAnalysis,
                                                                 Set<String> userSet) {
        try {
            String jsonPart = extractJsonFromAIResponse(aiAnalysis);
            JsonNode root = objectMapper.readTree(jsonPart);
            
            // 레시피 재료 집합(정규화)
            List<String> recipeIngredients = parseIngredients(recipe.getIngredients());
            Set<String> recipeSet = recipeIngredients.stream()
                    .filter(s -> s != null)
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            // match 배열 파싱
            List<String> match = new ArrayList<>();
            if (root.has("match") && root.get("match").isArray()) {
                root.get("match").forEach(n -> {
                    if (n.isTextual()) {
                        String v = n.asText();
                        if (v != null && recipeSet.contains(v.trim().toLowerCase(Locale.ROOT))) {
                            match.add(v);
                        }
                    }
                });
            }

            // mismatch 배열 파싱
            List<String> mismatch = new ArrayList<>();
            if (root.has("mismatch") && root.get("mismatch").isArray()) {
                root.get("mismatch").forEach(n -> {
                    if (n.isTextual()) {
                        String v = n.asText();
                        if (v != null && recipeSet.contains(v.trim().toLowerCase(Locale.ROOT))) {
                            mismatch.add(v);
                        }
                    }
                });
            }

            // replaceable 배열 파싱 (사용자 재료 존재 여부 검증 추가)
            List<RecipeResponse.ReplaceableIngredient> replaceable = new ArrayList<>();
            if (root.has("replaceable") && root.get("replaceable").isArray()) {
                root.get("replaceable").forEach(n -> {
                    if (n.has("recipeIngredient") && n.has("userIngredient")) {
                        String ri = n.get("recipeIngredient").asText(null);
                        String ui = n.get("userIngredient").asText(null);
                        if (ri != null && ui != null
                                && recipeSet.contains(ri.trim().toLowerCase(Locale.ROOT))
                                && userSet != null
                                && userSet.contains(ui.trim().toLowerCase(Locale.ROOT))) {
                            replaceable.add(new RecipeResponse.ReplaceableIngredient(ri, ui));
                        }
                    }
                });
            }

            // 중복 제거 및 상호 배타성 보장
            Set<String> matchSet = match.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            
            // 1) match와 중복되는 replaceable 제거 + 2) recipeIngredient 기준으로 replaceable dedup(첫 항목 유지)
            java.util.Map<String, RecipeResponse.ReplaceableIngredient> uniqueRepl = new java.util.LinkedHashMap<>();
            for (RecipeResponse.ReplaceableIngredient r : replaceable) {
                if (r == null || r.getRecipeIngredient() == null) continue;
                String k = r.getRecipeIngredient().trim().toLowerCase(Locale.ROOT);
                if (matchSet.contains(k)) continue; // match에 있으면 제외
                uniqueRepl.putIfAbsent(k, r);
            }
            List<RecipeResponse.ReplaceableIngredient> replaceableFiltered = new ArrayList<>(uniqueRepl.values());
            
            Set<String> replaceableRiSet = replaceableFiltered.stream()
                    .map(RecipeResponse.ReplaceableIngredient::getRecipeIngredient)
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            
            // mismatch에서 match와 replaceable의 recipeIngredient와 중복되는 항목 제거
            List<String> mismatchFiltered = mismatch.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> {
                        String k = s.toLowerCase(Locale.ROOT);
                        return !matchSet.contains(k) && !replaceableRiSet.contains(k);
                    })
                    .distinct()
                    .collect(Collectors.toList());

            // 누락 재료 보정: match/repl/mismatch 어디에도 없는 레시피 재료는 mismatch에 편입
            Set<String> mismatchNorm = mismatchFiltered.stream()
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            for (String ri : recipeIngredients) {
                if (ri == null || ri.isBlank()) continue;
                String k = ri.trim().toLowerCase(Locale.ROOT);
                if (!matchSet.contains(k) && !replaceableRiSet.contains(k) && !mismatchNorm.contains(k)) {
                    mismatchFiltered.add(ri);
                    mismatchNorm.add(k);
                }
            }

            return new RecipeResponse.IngredientMatchingRes(
                recipe.getId(), recipe.getTitle(), 
                match.stream().distinct().collect(Collectors.toList()), 
                mismatchFiltered, 
                replaceableFiltered
            );
        } catch (Exception e) {
            log.error("AI 분석 결과 파싱 실패: {}", e.getMessage());
            // analyzeIngredientsWithAI와 일관된 fallback 로직
            List<String> fallbackMismatch = parseIngredients(recipe.getIngredients());
            return new RecipeResponse.IngredientMatchingRes(
                recipe.getId(), recipe.getTitle(), List.of(), fallbackMismatch, List.of()
            );
        }
    }

        /**
     * 단순 문자열 매칭을 수행합니다.
     */
    private RecipeResponse.IngredientMatchingRes performSimpleMatching(Recipe recipe, List<String> recipeIngredients, List<String> userIngredients) {
        List<String> match = new ArrayList<>();
        List<String> mismatch = new ArrayList<>();
        
        Set<String> userSet = userIngredients.stream()
                .filter(s -> s != null)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        
        for (String recipeIngredient : recipeIngredients) {
            String key = recipeIngredient == null ? "" : recipeIngredient.trim().toLowerCase(Locale.ROOT);
            if (userSet.contains(key)) {
                match.add(recipeIngredient);
            } else {
                mismatch.add(recipeIngredient);
            }
        }
        
        return new RecipeResponse.IngredientMatchingRes(
            recipe.getId(), recipe.getTitle(), match, mismatch, new ArrayList<>()
        );
    }





    /**
     * AI 응답에서 JSON 부분을 추출합니다.
     */
    private String extractJsonFromAIResponse(String aiContent) {
        try {
            // JSON 블록 찾기
            int startIndex = aiContent.indexOf('{');
            int endIndex = aiContent.lastIndexOf('}');
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                return aiContent.substring(startIndex, endIndex + 1);
            }
            
            return aiContent;
        } catch (Exception e) {
            log.warn("JSON 추출 실패: {}", e.getMessage());
            return aiContent;
        }
    }

    /**
     * 사용자의 레시피 찜 여부를 일괄 조회합니다.
     */
    private Map<Long, Boolean> getUserRecipeLikedMap(Long userId, List<Long> recipeIds) {
        try {
            List<UserRecipe> userRecipes = userRecipeRepository.findByUserIdAndRecipeIdIn(userId, recipeIds);
            Map<Long, Boolean> likedMap = new HashMap<>();
            
            // 모든 레시피에 대해 기본값 false 설정
            for (Long recipeId : recipeIds) {
                likedMap.put(recipeId, false);
            }
            
            // 찜한 레시피만 true로 업데이트
            for (UserRecipe userRecipe : userRecipes) {
                likedMap.put(userRecipe.getRecipe().getId(), Boolean.TRUE.equals(userRecipe.getLiked()));
            }
            
            return likedMap;
        } catch (Exception e) {
            log.warn("사용자 찜 여부 조회 실패: {} - {}", userId, e.getMessage());
            // 예외 발생 시에도 모든 레시피에 대해 false 반환
            Map<Long, Boolean> fallbackMap = new HashMap<>();
            for (Long recipeId : recipeIds) {
                fallbackMap.put(recipeId, false);
            }
            return fallbackMap;
        }
    }
}
