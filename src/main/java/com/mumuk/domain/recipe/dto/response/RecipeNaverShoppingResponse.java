package com.mumuk.domain.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@AllArgsConstructor
public class RecipeNaverShoppingResponse {

    private List<NaverShopping> naverShoppings;

    @Builder
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NaverShopping {
        private String title;
        private String price;
        private String link;
        private String imageUrl;
    }
}
