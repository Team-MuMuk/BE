package com.mumuk.domain.ingredient.controller;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingredient/register")
public class IngredientRegisterController {

    private final IngredientService ingredientService;

    public IngredientRegisterController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }
    @Operation(summary = "재료 등록", description = "입력하신 재료를 등록합니다.")
    @PostMapping
    public Response<String> registerIngredient(@Valid @RequestBody IngredientRequest.RegisterReq req, @AuthUser Long userId) {

        ingredientService.registerIngredient(req, userId);
        return Response.ok(ResultCode.INGREDIENT_REGISTER_OK, "재료 등록 성공");
    }
}
