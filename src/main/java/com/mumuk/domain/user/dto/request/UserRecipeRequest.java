package com.mumuk.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserRecipeRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ClickLikeReq{

        @NotBlank
        private Long recipeId;
    }
}
