package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;


public interface IngredientService {

    void registerIngredient(IngredientRegisterRequest dto,String accessToken);
}
