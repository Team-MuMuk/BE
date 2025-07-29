package com.mumuk.domain.recipe.entity;

import com.mumuk.domain.ingredient.entity.Ingredient;
import jakarta.persistence.*;
@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", insertable = false, updatable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", insertable = false, updatable = false)
    private Ingredient ingredient;


    // 생성자
    public RecipeIngredient() {}

    public RecipeIngredient(Long id, Long ingredientId, Long recipeId) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.recipeId = recipeId;
    }
    // Getters
    public Long getId() { return id; }
    public Long getIngredientId() { return ingredientId; }
    public Long getRecipeId() { return recipeId; }


    // Setters
    public void setId(Long id) { this.id = id; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
}
