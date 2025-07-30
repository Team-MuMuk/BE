package com.mumuk.domain.recipe.repository;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.entity.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // 여러 카테고리 중 하나라도 포함된 레시피의 이름 반환
    @Query("SELECT DISTINCT r.title FROM Recipe r JOIN r.categories c WHERE c IN :categories")
    List<String> findNamesByCategories(@Param("categories") List<RecipeCategory> categories);

    // 여러 카테고리 중 하나라도 포함된 레시피 반환
    @Query("SELECT DISTINCT r FROM Recipe r JOIN r.categories c WHERE c IN :categories")
    List<Recipe> findByCategoriesIn(@Param("categories") List<RecipeCategory> categories);

    // 입력한 레시피 id를 제외하고, 같은 카테고리 중 하나라도 포함된 레시피 6개를 랜덤하게 조회
    @Query("SELECT DISTINCT r FROM Recipe r JOIN r.categories c WHERE c IN :categories AND r.id != :recipeId ORDER BY FUNCTION('RANDOM')")
    List<Recipe> findRandomRecipesByCategories(@Param("categories") List<RecipeCategory> categories, @Param("recipeId") Long recipeId);

    // 칼로리 범위로 검색
    List<Recipe> findByCaloriesLessThanEqual(Long maxCalories);

    // 조리시간 범위로 검색
    List<Recipe> findByCookingTimeLessThanEqual(Long maxCookingTime);

    // 복합 조건 검색
    @Query("SELECT r FROM Recipe r WHERE " +
           "(:keyword IS NULL OR r.title LIKE %:keyword%) AND " +
           "(:categories IS NULL OR EXISTS (SELECT 1 FROM r.categories c WHERE c IN :categories)) AND " +
           "(:maxCalories IS NULL OR r.calories <= :maxCalories) AND " +
           "(:maxCookingTime IS NULL OR r.cookingTime <= :maxCookingTime)")
    List<Recipe> searchRecipes(@Param("keyword") String keyword,
                               @Param("categories") List<RecipeCategory> categories,
                               @Param("maxCalories") Long maxCalories,
                               @Param("maxCookingTime") Long maxCookingTime);

    // 중복 레시피 검증
    boolean existsByTitleAndIngredients(String title, String ingredients);
    
    // 제목만으로 중복 체크
    boolean existsByTitle(String title);

    // 제목과 재료로 중복 체크
    @Query("SELECT r FROM Recipe r WHERE r.title = :title AND r.ingredients = :ingredients")
    List<Recipe> findByTitleAndIngredients(@Param("title") String title, @Param("ingredients") String ingredients);

    // 제목으로 검색 (대소문자 무시, SimpleRes DTO로 반환)
    @Query("SELECT new com.mumuk.domain.recipe.dto.response.RecipeResponse$SimpleRes(r.id, r.title, r.recipeImage) FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<RecipeResponse.SimpleRes> findByTitleContainingIgnoreCase(@Param("title") String title);
}