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
import com.mumuk.domain.recipe.service.RecipeService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import com.mumuk.domain.recipe.converter.RecipeConverter;

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
    private final RecipeService recipeService;

    private static final Duration RECIPE_CACHE_TTL = Duration.ofDays(7); // 7일 동안 캐시

    public RecipeRecommendServiceImpl(OpenAiClient openAiClient, ObjectMapper objectMapper,
                                   UserRepository userRepository, IngredientService ingredientService,
                                   AllergyService allergyService, RecipeRepository recipeRepository,
                                   RedisTemplate<String, Object> redisTemplate,
                                   RecipeService recipeService) {
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.ingredientService = ingredientService;
        this.allergyService = allergyService;
        this.recipeRepository = recipeRepository;
        this.redisTemplate = redisTemplate;
        this.recipeService = recipeService;
    }

    // 1. recommendAndSaveRecipes는 전체 흐름만 담당하도록 정리
    @Override
    public List<RecipeResponse.DetailRes> recommendAndSaveRecipes(Long userId) {
        User user = getUser(userId);
        List<String> availableIngredients = getUserIngredients(userId);
        List<String> allergyTypes = getUserAllergies(userId);
        String redisKey = generateRedisKey(userId, availableIngredients, allergyTypes);
        List<RecipeResponse.DetailRes> cachedResult = getCachedRecommendations(redisKey);
        if (cachedResult != null) return cachedResult;
        String prompt = createRecommendationPrompt(availableIngredients, allergyTypes, user);
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
        List<String> allergyTypes = getUserAllergies(userId);
        String prompt = createRecommendationPrompt(availableIngredients, allergyTypes, user);
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

    private String generateRedisKey(Long userId, List<String> ingredients, List<String> allergies) {
        String ingredientsStr = String.join(",", ingredients);
        String allergiesStr = String.join(",", allergies);
        
        // Redis 키 길이 제한 (512자) 및 특수문자 처리
        String key = "recipe:recommend:" + userId + ":" + ingredientsStr + ":" + allergiesStr;
        
        // 키가 너무 길 경우 해시 사용
        if (key.length() > 200) {
            return "recipe:recommend:" + userId + ":" + ingredientsStr.hashCode() + ":" + allergiesStr.hashCode();
        }
        
        return key;
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
                                           List<String> allergyTypes, 
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
        
        // 1. Redis에서 중복 검사
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
                
                // 저장 성공 시 Redis에 캐싱
                cacheRecipeToRedis(savedRecipe);
                
            } catch (Exception e) {
                log.warn("레시피 저장 실패: {} - {}", recipe.getTitle(), e.getMessage());
            }
        }
        
        return savedRecipes;
    }

    /**
     * 레시피를 Redis에 캐싱
     */
    private void cacheRecipeToRedis(Recipe recipe) {
        try {
            String redisKey = "recipe:title:" + recipe.getTitle().hashCode();
            redisTemplate.opsForValue().set(redisKey, recipe, Duration.ofDays(30));
            log.info("레시피 Redis 캐싱 성공: {}", recipe.getTitle());
        } catch (Exception e) {
            log.warn("레시피 Redis 캐싱 실패: {}", e.getMessage());
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
        if (apiKey == null || apiKey.equals("dummy-openai-key")) {
            log.error("API 키가 설정되지 않았습니다.");
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
} 