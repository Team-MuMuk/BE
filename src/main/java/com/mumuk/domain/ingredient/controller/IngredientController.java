package com.mumuk.domain.ingredient.controller;

import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingredient/register")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @Operation(summary = "재료 등록", description = "입력하신 재료를 등록합니다.")
    @PostMapping
    public Response<String> registerIngredient(@Valid @RequestBody IngredientRegisterRequest dto, HttpServletRequest request) {
        String accessToken = request.getHeader("Authorization");
        ingredientService.registerIngredient(dto, accessToken);
        return Response.ok(ResultCode.INGREDIENT_REGISTER_OK, "재료 등록 성공");
    }
}
