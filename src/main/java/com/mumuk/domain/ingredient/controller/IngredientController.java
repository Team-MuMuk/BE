package com.mumuk.domain.ingredient.controller;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredient")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }
    @Operation(summary = "재료 등록", description = "입력하신 재료를 등록합니다.")
    @PostMapping("/get")
    public Response<String> registerIngredient (@Valid @RequestBody IngredientRequest.RegisterReq req, @AuthUser Long userId){

        ingredientService.registerIngredient(req, userId);
        return Response.ok(ResultCode.INGREDIENT_REGISTER_OK, "재료 등록 성공");
    }


    @Operation(summary = "재료 조회", description = "등록하신 재료를 조회합니다.")
    @GetMapping("/retrieve")
    public Response<List<IngredientResponse.RetrieveRes>> retrieveIngredient (@AuthUser Long userId){

        List<IngredientResponse.RetrieveRes> ingredients = ingredientService.getAllIngredient(userId);
        return Response.ok(ResultCode.INGREDIENT_RETRIEVE_OK, ingredients);
    }


    @Operation(summary = "재료 수정", description = "등록하신 재료의 상세정보를 수정합니다.")
    @PatchMapping("/{ingredientId}/update")
    public Response<String> updateIngredient(
            @PathVariable Long ingredientId,
            @Valid @RequestBody IngredientRequest.UpdateReq req,
            @AuthUser Long userId) {


        ingredientService.updateIngredient(ingredientId, req, userId);
        return Response.ok(ResultCode.INGREDIENT_UPDATE_OK, "재료 수정 완료");
    }

    @Operation(summary = "재료 삭제", description = "해당 재료를 삭제합니다.")
    @DeleteMapping("/{ingredientId}/delete")
    public Response<String> deleteIngredient(@PathVariable Long ingredientId, @AuthUser Long userId) {

        ingredientService.deleteIngredient(ingredientId, userId);
        return Response.ok(ResultCode.INGREDIENT_DELETE_OK, "재료 삭제 성공");
    }


}