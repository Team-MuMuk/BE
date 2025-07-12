package com.mumuk.domain.recipe.converter;

import com.mumuk.domain.recipe.dto.request.RecipeRequest;
import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import com.mumuk.domain.recipe.entity.RecipeImage;
import com.mumuk.domain.recipe.entity.RecipeIngredient;

import java.util.List;
import java.util.stream.Collectors;

public class RecipeConverter {

    // 등록용 DTO → 엔티티
    public static Recipe toRecipe(RecipeRequest.CreateReq req) {
        Recipe recipe = new Recipe();
        recipe.setName(req.getName());
        recipe.setDescription(req.getDescription());
        recipe.setCookingMinutes(req.getCookingMinutes());
        recipe.setProtein(req.getProtein());
        recipe.setCarbohydrate(req.getCarbohydrate());
        recipe.setFat(req.getFat());
        recipe.setCalories(req.getCalories());
        recipe.setCategory(RecipeCategory.valueOf(req.getCategory()));

        // 이미지
        if (req.getImageUrls() != null) {
            List<RecipeImage> images = req.getImageUrls().stream()
                    .map(url -> {
                        RecipeImage img = new RecipeImage();
                        img.setImageUrl(url);
                        img.setRecipe(recipe);
                        return img;
                    })
                    .collect(Collectors.toList());
            recipe.setImages(images);
        }

        // 재료 (실제 Ingredient 엔티티와 연동 필요)
        // recipe.setIngredients(...) 등 추가 구현 필요

        return recipe;
    }

    // 엔티티 → 상세 응답 DTO
    public static RecipeResponse.DetailRes toDetailRes(Recipe recipe) {
        List<String> imageUrls = recipe.getImages() != null
                ? recipe.getImages().stream().map(RecipeImage::getImageUrl).collect(Collectors.toList())
                : null;

        // 재료 변환 (실제 Ingredient 정보 필요)
        List<RecipeResponse.DetailRes.IngredientRes> ingredients = null;
        // TODO: RecipeIngredient와 Ingredient 연동 시 아래처럼 변환
        // ingredients = recipe.getIngredients().stream()
        //     .map(ri -> new RecipeResponse.DetailRes.IngredientRes(ri.getIngredient().getName(), "수량"))
        //     .collect(Collectors.toList());

        return new RecipeResponse.DetailRes(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getCookingMinutes(),
                recipe.getProtein(),
                recipe.getCarbohydrate(),
                recipe.getFat(),
                recipe.getCalories(),
                recipe.getCategory().name(),
                imageUrls,
                ingredients
        );
    }
}
