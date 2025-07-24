package com.mumuk.domain.user.controller;


import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.user.converter.OAuthConverter;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.service.MypageService;
import com.mumuk.domain.user.service.RecentRecipeService;
import com.mumuk.domain.user.service.UserRecipeService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.lettuce.core.dynamic.annotation.Param;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-recipe")
@Slf4j
public class UserRecipeController {

    private final UserRecipeService userRecipeService;


    public UserRecipeController(UserRecipeService userRecipeService) {

        this.userRecipeService = userRecipeService;
    }


    @Operation(summary = "해당 레시피 상세 조회 + 사용자의 해당 레시피 조회 여부를 저장")
    @PostMapping("/{recipeId}")
    public Response<UserRecipeResponse.UserRecipeRes> getUserRecipe(@AuthUser Long userId, @PathVariable Long recipeId) {
        UserRecipeResponse.UserRecipeRes response = userRecipeService.getUserRecipeDetail(userId,recipeId);
        return Response.ok(ResultCode.USER_RECIPE_OK, response);
    }


    @Operation(summary = "최근 레시피 조회", description = "사용자가 최근에 조회한 레시피 목록을 8개까지 조회합니다.")
    @GetMapping("/recent-recipe")
    public Response<UserRecipeResponse.RecentRecipeDTOList> getRecentRecipe(@AuthUser Long userId) {
        UserRecipeResponse.RecentRecipeDTOList response = userRecipeService.getRecentRecipes(userId);
        return Response.ok(ResultCode.RECENT_RECIPE_OK, response);
    }
}
