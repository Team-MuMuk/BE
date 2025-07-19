package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;

import java.util.List;


public interface IngredientService {
    void registerIngredient(IngredientRequest.RegisterReq req, Long userId);
    List<IngredientResponse.RetrieveRes> getAllIngredient(Long userId);
    void updateIngredient(Long ingredientId, IngredientRequest.UpdateReq req, Long userId);
    void deleteIngredient(Long ingredientId, Long userId);
}
