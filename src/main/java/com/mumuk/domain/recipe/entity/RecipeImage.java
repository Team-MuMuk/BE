package com.mumuk.domain.recipe.entity;

import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "recipe_image")
public class RecipeImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;
}
