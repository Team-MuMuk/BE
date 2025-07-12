package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.service.RecipeService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipe")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Operation(summary = "레시피 등록")
    @PostMapping
    public Response<String> createRecipe(@RequestBody RecipeRequest.CreateReq request) {
        recipeService.createRecipe(request);
        return Response.ok(ResultCode.RECIPE_CREATE_OK, "레시피 등록 완료");
    }

    @Operation(summary = "레시피 삭제")
    @DeleteMapping("/{id}")
    public Response<String> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return Response.ok(ResultCode.RECIPE_DELETE_OK, "레시피 삭제 완료");
    }

    @Operation(summary = "레시피 상세 조회")
    @GetMapping("/{id}")
    public Response<RecipeResponse.DetailRes> getRecipe(@PathVariable Long id) {
        RecipeResponse.DetailRes response = recipeService.getRecipeDetail(id);
        return Response.ok(ResultCode.RECIPE_FETCH_OK, response);
    }
}