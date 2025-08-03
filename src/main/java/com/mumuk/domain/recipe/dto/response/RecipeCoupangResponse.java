package com.mumuk.domain.recipe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class RecipeCoupangResponse {

    private List<Coupang> coupangs;

    @Getter
    @AllArgsConstructor
    public static class Coupang{
        private String title;
        private String price;
        private String link;
        private String imageUrl;
    }
}
