package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientRegisterResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;

public interface IngredientService {
    void registerIngredient(IngredientRegisterRequest dto);
}
