package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.service.RecipeRecommendService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recipe/recommend")
@Tag(name = "recipe-recommend-controller", description = "레시피 추천 API")
public class RecipeRecommendController {
    private final RecipeRecommendService recommendService;

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

    @Operation(summary = "재료 기반 레시피 추천")
    @GetMapping("/ingredient")
    public Response<List<RecipeResponse.SimpleRes>> recommendRecipesByIngredient(@AuthUser Long userId) {
        List<RecipeResponse.SimpleRes> result = recommendService.recommendRecipesByIngredient(userId);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }

    @Operation(summary = "건강 정보 기반 레시피 추천")
    @GetMapping("/health")
    public Response<List<RecipeResponse.SimpleRes>> recommendRecipesByHealth(@AuthUser Long userId) {
        List<RecipeResponse.SimpleRes> result = recommendService.recommendRecipesByHealth(userId);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }

    @Operation(summary = "카테고리들에 해당하는 레시피 추천")
    @GetMapping("/categories/{categories}")
    public Response<List<RecipeResponse.SimpleRes>> recommendRecipesByCategories(@PathVariable String categories) {
        List<RecipeResponse.SimpleRes> result = recommendService.recommendRecipesByCategories(categories);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }

    @Operation(summary = "랜덤 레시피 추천")
    @GetMapping("/random")
    public Response<List<RecipeResponse.SimpleRes>> recommendRandomRecipes() {
        List<RecipeResponse.SimpleRes> result = recommendService.recommendRandomRecipes();
        return Response.ok(ResultCode.RECIPE_FETCH_OK, result);
    }


} 