package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.converter.RecipeConverter;
import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
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
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.healthManagement.service.AllergyService;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

@Service
public class RecipeServiceImpl implements RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeServiceImpl.class);
    private static final Duration RECIPE_CACHE_TTL = Duration.ofDays(7); // 7일 동안 캐시

    private final RecipeRepository recipeRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final IngredientService ingredientService;
    private final AllergyService allergyService;
    private final JwtTokenProvider jwtTokenProvider;

    public RecipeServiceImpl(RecipeRepository recipeRepository, RedisTemplate<String, Object> redisTemplate,
                           OpenAiClient openAiClient, ObjectMapper objectMapper, UserRepository userRepository,
                           IngredientService ingredientService, AllergyService allergyService, JwtTokenProvider jwtTokenProvider) {
        this.recipeRepository = recipeRepository;
        this.redisTemplate = redisTemplate;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.ingredientService = ingredientService;
        this.allergyService = allergyService;
        this.jwtTokenProvider = jwtTokenProvider;
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
    public List<RecipeResponse.SimpleRes> getSimpleRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        return recipes.stream()
                .map(RecipeConverter::toSimpleRes)
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
    public RecipeResponse.IngredientMatchingRes matchIngredientsByAI(Long recipeId) {
        Long userId = getCurrentUserId();
        log.info("AI 기반 재료 매칭 시작: 사용자 ID: {}, 레시피 ID: {}", userId, recipeId);
        
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
        
        List<String> userIngredients = getUserIngredients(userId);
        List<String> recipeIngredients = parseIngredients(recipe.getIngredients());
        
        log.info("사용자 재료: {}", userIngredients);
        log.info("레시피 재료: {}", recipeIngredients);
        
        // AI 분석 요청
        String aiAnalysis = analyzeIngredientsWithAI(userIngredients, recipeIngredients);
        log.info("AI 분석 결과: {}", aiAnalysis);
        
        // AI 분석 결과 파싱
        RecipeResponse.IngredientMatchingRes result = parseAIAnalysis(recipe, aiAnalysis);
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse.IngredientMatchingRes matchIngredientsSimple(Long recipeId) {
        Long userId = getCurrentUserId();
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
     * 현재 인증된 사용자의 ID를 가져옵니다.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // JWT 토큰에서 직접 userId 추출
        try {
            String token = extractTokenFromRequest();
            return jwtTokenProvider.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("사용자 ID 추출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }
    
    /**
     * 현재 요청에서 JWT 토큰을 추출합니다.
     */
    private String extractTokenFromRequest() {
        try {
            // HttpServletRequest를 가져오기 위해 RequestContextHolder 사용
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String bearerToken = request.getHeader("Authorization");
            
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken;
            }
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 사용자의 냉장고 재료 목록을 조회합니다.
     */
    private List<String> getUserIngredients(Long userId) {
        try {
            // IngredientService를 통해 사용자의 재료 목록 조회
            List<IngredientResponse.RetrieveRes> ingredients = ingredientService.getAllIngredient(userId);
            return ingredients.stream()
                    .map(IngredientResponse.RetrieveRes::getName)
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
            return response;
        } catch (Exception e) {
            log.error("AI 재료 매칭 분석 실패: {}", e.getMessage());
            // AI 실패 시 기본 JSON 형태로 반환
            return String.format("""
                {
                    "match": [],
                    "mismatch": %s,
                    "replaceable": []
                }
                """, recipeIngredients);
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
            return openAiClient.chat(prompt).block();
        } catch (Exception e) {
            log.error("AI 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 분석을 수행할 수 없습니다.", e);
        }
    }

    /**
     * AI 분석 결과를 파싱합니다.
     */
    private RecipeResponse.IngredientMatchingRes parseAIAnalysis(Recipe recipe, String aiAnalysis) {
        try {
            String jsonPart = extractJsonFromAIResponse(aiAnalysis);
            Map<String, Object> analysisMap = objectMapper.readValue(jsonPart, Map.class);
            
            // match 배열 파싱
            List<String> match = new ArrayList<>();
            if (analysisMap.containsKey("match")) {
                match = (List<String>) analysisMap.get("match");
            }
            
            // mismatch 배열 파싱
            List<String> mismatch = new ArrayList<>();
            if (analysisMap.containsKey("mismatch")) {
                mismatch = (List<String>) analysisMap.get("mismatch");
            }
            
            // replaceable 배열 파싱
            List<RecipeResponse.ReplaceableIngredient> replaceable = new ArrayList<>();
            if (analysisMap.containsKey("replaceable")) {
                List<Map<String, Object>> replaceableList = (List<Map<String, Object>>) analysisMap.get("replaceable");
                                 for (Map<String, Object> item : replaceableList) {
                     replaceable.add(new RecipeResponse.ReplaceableIngredient(
                         (String) item.get("recipeIngredient"),
                         (String) item.get("userIngredient")
                     ));
                 }
            }
            
            return new RecipeResponse.IngredientMatchingRes(
                recipe.getId(), recipe.getTitle(), match, mismatch, replaceable
            );
        } catch (Exception e) {
            log.error("AI 분석 결과 파싱 실패: {}", e.getMessage());
            return new RecipeResponse.IngredientMatchingRes(
                recipe.getId(), recipe.getTitle(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
            );
        }
    }

        /**
     * 단순 문자열 매칭을 수행합니다.
     */
    private RecipeResponse.IngredientMatchingRes performSimpleMatching(Recipe recipe, List<String> recipeIngredients, List<String> userIngredients) {
        List<String> match = new ArrayList<>();
        List<String> mismatch = new ArrayList<>();
        
        for (String recipeIngredient : recipeIngredients) {
            boolean found = false;
            
            for (String userIngredient : userIngredients) {
                if (recipeIngredient.equals(userIngredient)) {
                    match.add(recipeIngredient);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
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
}
