package com.mumuk.domain.ingredient.repository;

import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    List<Ingredient> findAllByUser(User user);
    List<Ingredient> findByUserAndExpireDateBetweenOrderByExpireDateAsc(User user, LocalDate start, LocalDate end);
}
