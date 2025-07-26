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

    @Override
    public RecipeResponse.AiRecommendListDto recommendAndSaveRecipes(RecipeRequest.AiRecommendReq request) {
        // 사용자 정보 조회
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 기존 API를 사용하여 사용자의 냉장고 재료 조회
        List<IngredientResponse.RetrieveRes> userIngredients = ingredientService.getAllIngredient(request.getUserId());
        List<String> availableIngredients = userIngredients.stream()
                .map(IngredientResponse.RetrieveRes::getName)
                .collect(Collectors.toList());

        // 기존 API를 사용하여 사용자의 알러지 정보 조회
        AllergyResponse.AllergyListRes allergyList = allergyService.getAllergyList(request.getUserId());
        List<String> allergyTypes = allergyList.getAllergyOptions().stream()
                .map(allergyOption -> allergyOption.getAllergyType().name())
                .collect(Collectors.toList());

        // Redis 키 생성 (사용자별 고유 키)
        String redisKey = generateRedisKey(request.getUserId(), availableIngredients, allergyTypes);
        
        // Redis에서 기존 추천 결과 확인
        RecipeResponse.AiRecommendListDto cachedResult = getCachedRecommendations(redisKey);
        if (cachedResult != null) {
            log.info("Redis에서 기존 추천 결과 조회: userId={}", request.getUserId());
            return cachedResult;
        }

        // AI 프롬프트 생성
        String prompt = createRecommendationPrompt(availableIngredients, allergyTypes, user);
        
        // AI 호출 및 응답 처리
        List<RecipeResponse.AiRecommendDto> recommendations = callAIAndProcessRecommendations(prompt);
        
        // 추천된 레시피들을 Recipe 엔티티로 변환하여 저장
        List<Recipe> savedRecipes = saveRecipesWithDuplicateCheck(recommendations);
        
        // 실제 저장된 레시피들만 포함한 결과 생성
        List<RecipeResponse.AiRecommendDto> savedRecommendations = recommendations.stream()
                .filter(rec -> savedRecipes.stream()
                        .anyMatch(saved -> saved.getTitle().equals(rec.getTitle()) && 
                                        saved.getIngredients().equals(rec.getIngredients())))
                .collect(Collectors.toList());
        
        RecipeResponse.AiRecommendListDto result = new RecipeResponse.AiRecommendListDto(savedRecommendations, 
                "AI가 추천한 " + savedRecipes.size() + "개의 레시피가 저장되었습니다.");
        
        // Redis에 결과 캐싱 (실제 저장된 결과만)
        cacheRecommendations(redisKey, result);
        
        log.info("AI 레시피 추천 및 저장 완료: {}개의 레시피 저장", savedRecipes.size());
        
        return result;
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

    private RecipeResponse.AiRecommendListDto getCachedRecommendations(String redisKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                return (RecipeResponse.AiRecommendListDto) cached;
            }
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: {}", e.getMessage());
        }
        return null;
    }

    private void cacheRecommendations(String redisKey, RecipeResponse.AiRecommendListDto result) {
        try {
            redisTemplate.opsForValue().set(redisKey, result, RECIPE_CACHE_TTL);
            log.info("Redis에 추천 결과 캐싱: {}", redisKey);
        } catch (Exception e) {
            log.warn("Redis 캐싱 실패: {}", e.getMessage());
        }
    }

    /**
     * 배치로 중복 체크하여 N+1 문제 해결
     */
    private Set<String> checkDuplicatesBatch(List<RecipeResponse.AiRecommendDto> recommendations) {
        Set<String> duplicateKeys = new HashSet<>();
        
        try {
            // 제목과 재료 조합을 Object[] 배열로 변환
            List<Object[]> titleIngredientPairs = recommendations.stream()
                    .map(rec -> new Object[]{rec.getTitle(), rec.getIngredients()})
                    .collect(Collectors.toList());
            
            // 배치로 중복 체크 (한 번의 쿼리로 모든 조합 확인)
            List<Recipe> existingRecipes = recipeRepository.findByTitleAndIngredientsPairs(titleIngredientPairs);
            
            // 중복된 조합들을 Set으로 저장
            for (Recipe existingRecipe : existingRecipes) {
                duplicateKeys.add(existingRecipe.getTitle() + "|" + existingRecipe.getIngredients());
            }
            
        } catch (Exception e) {
            log.warn("배치 중복 체크 실패: {}", e.getMessage());
            // 예외 발생 시 모든 레시피를 중복으로 처리하여 안전하게 처리
            for (RecipeResponse.AiRecommendDto recommendation : recommendations) {
                duplicateKeys.add(recommendation.getTitle() + "|" + recommendation.getIngredients());
            }
        }
        
        return duplicateKeys;
    }

    private List<Recipe> saveRecipesWithDuplicateCheck(List<RecipeResponse.AiRecommendDto> recommendations) {
        List<Recipe> savedRecipes = new ArrayList<>();
        
        // 배치로 중복 체크 (N+1 문제 해결)
        Set<String> duplicateKeys = checkDuplicatesBatch(recommendations);
        
        // 중복되지 않은 레시피들만 필터링
        List<Recipe> recipesToSave = recommendations.stream()
                .filter(rec -> !duplicateKeys.contains(rec.getTitle() + "|" + rec.getIngredients()))
                .map(this::convertToRecipe)
                .collect(Collectors.toList());
        
        try {
            // 배치 저장 (성능 최적화)
            if (!recipesToSave.isEmpty()) {
                List<Recipe> savedBatch = recipeRepository.saveAll(recipesToSave);
                savedRecipes.addAll(savedBatch);
                log.info("배치 저장 완료: {}개의 레시피 저장", savedBatch.size());
            }
        } catch (Exception e) {
            log.error("배치 저장 실패: {}", e.getMessage());
            
            // 배치 저장 실패 시 개별 저장으로 폴백
            for (Recipe recipe : recipesToSave) {
                try {
                    Recipe savedRecipe = recipeRepository.save(recipe);
                    savedRecipes.add(savedRecipe);
                    log.info("개별 레시피 저장: {}", savedRecipe.getTitle());
                } catch (Exception individualException) {
                    // 유니크 제약조건 위반 시 중복으로 처리
                    if (individualException.getMessage().contains("Duplicate entry") || 
                        individualException.getMessage().contains("unique constraint")) {
                        log.info("중복 레시피로 인한 저장 실패: {}", recipe.getTitle());
                    } else {
                        log.error("레시피 저장 실패: {}", individualException.getMessage());
                    }
                }
            }
        }
        
        return savedRecipes;
    }

    private String createRecommendationPrompt(List<String> availableIngredients, 
                                           List<String> allergyTypes, 
                                           User user) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("사용자가 가진 재료와 알러지 정보를 기반으로 5개의 레시피를 추천해주세요.\n\n");
        
        promptBuilder.append("사용자 정보:\n");
        promptBuilder.append("- 이름: ").append(user.getNickName()).append("\n");
        promptBuilder.append("- 사용 가능한 재료: ").append(String.join(", ", availableIngredients)).append("\n");
        promptBuilder.append("- 알러지 정보: ").append(String.join(", ", allergyTypes)).append("\n\n");
        
        promptBuilder.append("주의사항:\n");
        promptBuilder.append("- 알러지가 있는 재료는 절대 사용하지 마세요\n");
        promptBuilder.append("- 사용자가 가진 재료를 최대한 활용하세요\n");
        promptBuilder.append("- 알러지 정보가 NONE인 경우 알러지가 없다는 의미입니다\n");
        promptBuilder.append("- 기존에 많이 알려진 레시피보다는 창의적이고 새로운 레시피를 추천해주세요\n\n");
        
        promptBuilder.append("다음 JSON 형식으로 5개의 레시피를 추천해주세요:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"recommendations\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"title\": \"레시피 제목\",\n");
        promptBuilder.append("      \"description\": \"레시피 설명\",\n");
        promptBuilder.append("      \"ingredients\": \"필요한 재료들\",\n");
        promptBuilder.append("      \"estimatedCalories\": 300,\n");
        promptBuilder.append("      \"estimatedProtein\": 20,\n");
        promptBuilder.append("      \"estimatedCarbohydrate\": 30,\n");
        promptBuilder.append("      \"estimatedFat\": 10,\n");
        promptBuilder.append("      \"category\": \"한식\",\n");
        promptBuilder.append("      \"reason\": \"추천 이유\",\n");
        promptBuilder.append("      \"nutritionNotes\": \"영양 정보\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n");
        
        return promptBuilder.toString();
    }

    private List<RecipeResponse.AiRecommendDto> callAIAndProcessRecommendations(String prompt) {
        try {
            // AI 호출
            Mono<String> aiResponse = openAiClient.chat(prompt);
            String response = aiResponse.block();

            // JSON 파싱
            JsonNode root = objectMapper.readTree(response);
            
            List<RecipeResponse.AiRecommendDto> recommendations = new ArrayList<>();
            JsonNode recommendationsNode = root.path("recommendations");
            
            // AI 응답 검증
            if (recommendationsNode.isMissingNode() || !recommendationsNode.isArray()) {
                log.error("AI 응답에서 recommendations 배열을 찾을 수 없습니다: {}", response);
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }
            
            for (JsonNode rec : recommendationsNode) {
                try {
                    // 필수 필드 검증
                    String title = rec.path("title").asText();
                    String description = rec.path("description").asText();
                    String ingredients = rec.path("ingredients").asText();
                    String category = rec.path("category").asText();
                    
                    if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || category.isEmpty()) {
                        log.warn("AI 응답에서 필수 필드가 누락되었습니다: {}", rec.toString());
                        continue; // 해당 레시피 건너뛰기
                    }
                    
                    RecipeResponse.AiRecommendDto dto = new RecipeResponse.AiRecommendDto(
                            title,
                            description,
                            ingredients,
                            "", // cookingSteps
                            0L, // estimatedCookingTime
                            rec.path("estimatedCalories").asLong(0),
                            rec.path("estimatedProtein").asLong(0),
                            rec.path("estimatedCarbohydrate").asLong(0),
                            rec.path("estimatedFat").asLong(0),
                            "", // difficulty
                            category,
                            rec.path("reason").asText(""),
                            rec.path("nutritionNotes").asText(""),
                            "", // introMent
                            "", // mainIngredients
                            "", // nutritionInfo
                            ""  // cookingInfo
                    );
                    recommendations.add(dto);
                } catch (Exception e) {
                    log.warn("AI 응답 파싱 중 오류 발생, 해당 레시피 건너뛰기: {}", e.getMessage());
                }
            }
            
            if (recommendations.isEmpty()) {
                log.error("유효한 레시피 추천이 없습니다: {}", response);
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("AI 레시피 추천 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }

    private Recipe convertToRecipe(RecipeResponse.AiRecommendDto aiRecipe) {
        Recipe recipe = new Recipe();
        recipe.setTitle(aiRecipe.getTitle());
        recipe.setDescription(aiRecipe.getDescription());
        recipe.setIngredients(aiRecipe.getIngredients());
        recipe.setCookingTime(0L); // 기본값 설정
        recipe.setCalories(aiRecipe.getEstimatedCalories());
        recipe.setProtein(aiRecipe.getEstimatedProtein());
        recipe.setCarbohydrate(aiRecipe.getEstimatedCarbohydrate());
        recipe.setFat(aiRecipe.getEstimatedFat());
        recipe.setCategory(RecipeCategory.valueOf(aiRecipe.getCategory()));
        recipe.setSourceUrl("AI_RECOMMENDED");
        recipe.setRecipeImage("default_recipe_image.jpg"); // 기본 이미지
        
        return recipe;
    }
} 