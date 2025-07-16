package com.mumuk.domain.recipe.entity;

import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipe")
public class Recipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "레시피명", nullable = false)
    private String title;

    @Column(name = "레시피 대표 이미지", nullable = false, length = 150)
    private String recipeImage;

    @Column(name = "레시피 간단 소개", nullable = false, length = 100)
    private String description;

    @Column(name = "조리 소요 시간(n분)", nullable = false)
    private Long cookingTime;

    @Column(name = "소모 칼로리", nullable = false)
    private Long calories;

    @Column(name = "단백질", nullable = false)
    private Long protein;

    @Column(name = "탄수화물", nullable = false)
    private Long carbohydrate;

    @Column(name = "지방", nullable = false)
    private Long fat;

    @Column(name = "열량", nullable = false)
    private Long totalCalories;

    @Enumerated(EnumType.STRING)
    @Column(name = "카테고리", nullable = false)
    private RecipeCategory category;

    @Column(name = "출처 url", nullable = false, length = 150)
    private String sourceUrl;

    @Column(name = "재료 문자열", nullable = false, length = 200)
    private String ingredients;

    // Getter
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRecipeImage() {
        return recipeImage;
    }

    public String getDescription() {
        return description;
    }

    public Long getCookingTime() {
        return cookingTime;
    }

    public Long getCalories() {
        return calories;
    }

    public Long getProtein() {
        return protein;
    }

    public Long getCarbohydrate() {
        return carbohydrate;
    }

    public Long getFat() {
        return fat;
    }

    public Long getTotalCalories() {
        return totalCalories;
    }

    public RecipeCategory getCategory() {
        return category;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getIngredients() {
        return ingredients;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setRecipeImage(String recipeImage) {
        this.recipeImage = recipeImage;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCookingTime(Long cookingTime) {
        this.cookingTime = cookingTime;
    }

    public void setCalories(Long calories) {
        this.calories = calories;
    }

    public void setProtein(Long protein) {
        this.protein = protein;
    }

    public void setCarbohydrate(Long carbohydrate) {
        this.carbohydrate = carbohydrate;
    }

    public void setFat(Long fat) {
        this.fat = fat;
    }

    public void setTotalCalories(Long totalCalories) {
        this.totalCalories = totalCalories;
    }

    public void setCategory(RecipeCategory category) {
        this.category = category;
    }

    public void setSourceUrl(String sourseUrl) {
        this.sourceUrl = sourseUrl;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
}
