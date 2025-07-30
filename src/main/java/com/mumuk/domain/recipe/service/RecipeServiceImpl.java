package com.mumuk.domain.recipe.service;

import com.mumuk.domain.recipe.converter.RecipeConverter;
import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public void createRecipe(RecipeRequest.CreateReq request) {
        // 중복 레시피 검증
        if (recipeRepository.existsByTitleAndIngredients(request.getTitle(), request.getIngredients())) {
            throw new BusinessException(ErrorCode.RECIPE_DUPLICATE_TITLE);
        }
        
        Recipe recipe = RecipeConverter.toRecipe(request);
        recipeRepository.save(recipe);
    }

    @Override
    @Transactional
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.RECIPE_NOT_FOUND);
        }
        recipeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse.DetailRes getRecipeDetail(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
        return RecipeConverter.toDetailRes(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findNamesByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        RecipeCategory recipeCategory;
        try {
            recipeCategory = RecipeCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        return recipeRepository.findNamesByCategories(List.of(recipeCategory));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findNamesByCategories(String categories) {
        if (categories == null || categories.isBlank()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        
        String[] categoryArray = categories.split(",");
        List<RecipeCategory> recipeCategories = new ArrayList<>();
        
        for (String category : categoryArray) {
            try {
                RecipeCategory recipeCategory = RecipeCategory.valueOf(category.trim().toUpperCase());
                recipeCategories.add(recipeCategory);
            } catch (IllegalArgumentException e) {
                // 잘못된 카테고리는 무시하고 계속 진행
                continue;
            }
        }
        
        if (recipeCategories.isEmpty()) {
            throw new BusinessException(ErrorCode.RECIPE_CATEGORY_NOT_FOUND);
        }
        
        return recipeRepository.findNamesByCategories(recipeCategories);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponse.DetailRes> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        return recipes.stream()
                .map(RecipeConverter::toDetailRes)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeResponse.SimpleRes> getSimpleRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        return recipes.stream()
                .map(RecipeConverter::toSimpleRes)
                .collect(Collectors.toList());
    }
}
