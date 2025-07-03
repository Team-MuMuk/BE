package com.mumuk.domain.recipe.entity;


import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient id", nullable = false)
    private Ingredient ingredient;

}
