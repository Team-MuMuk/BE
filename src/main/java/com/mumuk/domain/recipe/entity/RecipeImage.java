package com.mumuk.domain.recipe.entity;

import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recipe_image")
public class RecipeImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // getter도 필요하면 추가
    // --- setter 추가 ---
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    // Getter 메서드들
    public Long getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    // Setter 메서드들 (필요한 경우에만)
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
