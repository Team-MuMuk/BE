package com.mumuk.domain.ingredient.repository;

import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByUser(User user);
    List<Ingredient> findByUserAndExpireDateBetweenOrderByExpireDateAsc(User user, LocalDate start, LocalDate end);
    
    // 사용자별 재료 조회 (AI 추천용)
    List<Ingredient> findByUser(User user);
}
