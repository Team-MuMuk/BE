package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.service.RecipeRecommendService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recipe/recommend")
public class RecipeRecommendController {
    private final RecipeRecommendService recommendService;
    public RecipeRecommendController(RecipeRecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @Operation(summary = "재료 기반 레시피 추천")
    @PostMapping("/ingredient")
    public Response<List<RecipeResponse.DetailRes>> recommendByIngredient(@AuthUser Long userId) {
        List<RecipeResponse.DetailRes> result = recommendService.recommendByIngredient(userId);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }

    @Operation(summary = "랜덤 레시피 추천")
    @PostMapping("/random")
    public Response<List<RecipeResponse.DetailRes>> recommendRandom() {
        List<RecipeResponse.DetailRes> result = recommendService.recommendRandom();
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }

    @Operation(summary = "건강 정보 기반 레시피 추천 (개발 예정)")
    @PostMapping("/health")
    public Response<List<RecipeResponse.DetailRes>> recommendByHealth() {
        throw new UnsupportedOperationException("건강 기반 추천 기능은 현재 개발 중입니다.");
    }
} 