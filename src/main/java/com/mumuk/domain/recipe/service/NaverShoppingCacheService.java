package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.dto.response.RecipeNaverShoppingResponse;
import org.openqa.selenium.WebDriver;

import java.util.List;

public interface NaverShoppingCacheService {
    List<RecipeNaverShoppingResponse.NaverShopping> fetchAndCacheProduct(String ingredient, String url);
    List<RecipeNaverShoppingResponse.NaverShopping> getCachedProduct(String url);
}
