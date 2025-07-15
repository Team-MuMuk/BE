package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientRegisterResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.user.dto.response.UserResponse;
import jakarta.transaction.Transactional;

public interface IngredientService {
    @Transactional
    UserResponse.ProfileInfoDTO profileInfo(Long userId);

    void registerIngredient(IngredientRegisterRequest dto);
}
