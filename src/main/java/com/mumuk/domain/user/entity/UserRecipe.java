package com.mumuk.domain.user.entity;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.global.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_recipe")
public class UserRecipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "찜한 레시피", nullable = false)
    private Boolean liked;

    @Column(name = "레시피 조회 여부")
    private Boolean viewed = false;

    @Column(name = "레시피 조회 시간")
    private LocalDateTime viewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;


    // Getter
    public Long getId() {
        return id;
    }

    public Boolean getLiked() {
        return liked;
    }

    public Boolean getViewed() {
        return viewed;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public User getUser() {
        return user;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
