package com.mumuk.domain.recipe.repository;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT r.title FROM Recipe r WHERE r.category = :category")
    List<String> findNamesByCategory(@Param("category") RecipeCategory category);

    // 제목으로 검색
    List<RecipeResponse.SimpleRes> findByTitleContainingIgnoreCase(String title);

    // 카테고리로 검색
    List<Recipe> findByCategory(RecipeCategory category);

    // 입력한 레시피 id를 제외하고, 같은 카테고리의 레시피 6개를 랜덤하게 조회
    @Query(value="SELECT * FROM recipe WHERE category= :category AND id != :recipeId ORDER BY RANDOM() LIMIT 6",nativeQuery=true)
    List<Recipe> findRandomRecipesByCategory(String category, Long recipeId);

    // 칼로리 범위로 검색
    List<Recipe> findByCaloriesLessThanEqual(Long maxCalories);

    // 조리시간 범위로 검색
    List<Recipe> findByCookingTimeLessThanEqual(Long maxCookingTime);

    // 복합 조건 검색
    @Query("SELECT r FROM Recipe r WHERE " +
           "(:keyword IS NULL OR r.title LIKE %:keyword%) AND " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:maxCalories IS NULL OR r.calories <= :maxCalories) AND " +
           "(:maxCookingTime IS NULL OR r.cookingTime <= :maxCookingTime)")
    List<Recipe> searchRecipes(
            @Param("keyword") String keyword,
            @Param("category") RecipeCategory category,
            @Param("maxCalories") Long maxCalories,
            @Param("maxCookingTime") Long maxCookingTime
    );

    // 제목과 재료로 중복 체크
    @Query("SELECT r FROM Recipe r WHERE r.title = :title AND r.ingredients = :ingredients")
    List<Recipe> findByTitleAndIngredients(@Param("title") String title, @Param("ingredients") String ingredients);

    // 배치 중복 체크를 위한 메서드
    @Query("SELECT r FROM Recipe r WHERE (r.title, r.ingredients) IN :titleIngredientPairs")
    List<Recipe> findByTitleAndIngredientsPairs(@Param("titleIngredientPairs") List<Object[]> titleIngredientPairs);
}