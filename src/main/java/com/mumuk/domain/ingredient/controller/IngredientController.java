package com.mumuk.domain.ingredient.controller;

import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.service.IngredientService;
import com.mumuk.domain.ingredient.service.IngredientServiceImpl;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.service.MypageService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingredient/register")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @PostMapping
    public Response<String> registerIngredient(@Valid @RequestBody IngredientRegisterRequest dto) {

        ingredientService.registerIngredient(dto);
        return Response.ok(ResultCode.INGREDIENT_REGISTER_OK, "재료 등록 성공");
    }
}