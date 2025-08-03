package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.response.RecipeBlogResponse;
import com.mumuk.domain.recipe.dto.response.RecipeCoupangResponse;
import com.mumuk.domain.recipe.service.RecipeBlogService;
import com.mumuk.domain.recipe.service.RecipeCoupangService;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/recipe")
public class RecipeCoupangController {

    private final RecipeCoupangService recipeCoupangService;

    public RecipeCoupangController(RecipeCoupangService recipeCoupangService) {
        this.recipeCoupangService = recipeCoupangService;
    }

    @Operation(summary = "쿠팡 재료 검색", description = "레시피에 필요한 재료중 사용자에게 없는 재료를 쿠팡에서 검색합니다.")
    @GetMapping("/search-coupang")
    public RecipeCoupangResponse searchCoupang(@AuthUser Long userId,@PathVariable Long recipeId) {
        return recipeCoupangService.searchCoupang(userId, recipeId);
    }
}
