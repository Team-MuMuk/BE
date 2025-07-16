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
}