package com.mumuk.domain.user.repository;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.entity.UserRecipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRecipeRepository extends JpaRepository<UserRecipe, Long> {
    Optional<UserRecipe> findByUserIdAndRecipeId(Long userId, Long recipeId);
    @EntityGraph(attributePaths = {"recipe"})
    List<UserRecipe> findByUserIdAndRecipeIdIn(Long userId, List<Long> recipeIds);

}
