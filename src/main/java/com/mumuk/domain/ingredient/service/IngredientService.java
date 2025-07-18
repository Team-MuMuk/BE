package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;


public interface IngredientService {

    void registerIngredient(IngredientRequest.RegisterReq req, Long userId);
}
