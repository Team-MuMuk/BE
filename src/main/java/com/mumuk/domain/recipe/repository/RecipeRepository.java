package com.mumuk.domain.recipe.repository;

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
    List<Recipe> findByTitleContainingIgnoreCase(String title);

    // 카테고리로 검색
    List<Recipe> findByCategory(RecipeCategory category);

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
}