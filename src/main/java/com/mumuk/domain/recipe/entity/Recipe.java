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

    @Column(name = "레시피 이름", nullable = false)
    private String name;

    @Column(name = "레피시 간단 소개", nullable = false)
    private String description;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeImage> images = new ArrayList<>();

    @Column(name = "조리 소요 시간(n분)", nullable = false)
    private Long cookingMinutes;

    @Column(name = "단백질", nullable = false)
    private Long protein;

    @Column(name = "탄수화물", nullable = false)
    private Long carbohydrate;

    @Column(name = "지방", nullable = false)
    private Long fat;

    @Column(name = "열량", nullable = false)
    private Long calories;

    @Enumerated(EnumType.STRING)
    private RecipeCategory category;

    // Getter
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<RecipeImage> getImages() {
        return images;
    }

    public Long getCookingMinutes() {
        return cookingMinutes;
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

    public Long getCalories() {
        return calories;
    }

    public RecipeCategory getCategory() {
        return category;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImages(List<RecipeImage> images) {
        this.images = images;
    }

    public void setCookingMinutes(Long cookingMinutes) {
        this.cookingMinutes = cookingMinutes;
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

    public void setCalories(Long calories) {
        this.calories = calories;
    }

    public void setCategory(RecipeCategory category) {
        this.category = category;
    }
}
