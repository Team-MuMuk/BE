package com.mumuk.domain.user.controller;



import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.user.dto.request.UserRecipeRequest;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;

import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.service.UserRecipeService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
    @GetMapping("/{recipeId}")
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

    @Operation(summary = "레시피 찜버튼(하트) 클릭", description = "레시피의 하트를 클릭하면 찜 상태가 바뀝니다.")
    @PostMapping("/click-like")
    public Response<String> clickLike(@AuthUser Long userId,@RequestBody UserRecipeRequest.ClickLikeReq req) {
        userRecipeService.clickLike(userId, req);
        return Response.ok(ResultCode.CLICK_LIKE_OK,"찜 상태가 변경되었습니다.");
    }

    //찜한 레시피 조회
    @Operation(summary = "찜한 레시피 조회", description = "사용자가 찜한 레시피를 조회합니다.")
    @GetMapping("/liked-recipe")
    public Response<UserRecipeResponse.LikedRecipeListDTO> likedRecipe(@AuthUser Long userId,@Valid @RequestParam(defaultValue = "1") int page) {
        return Response.ok(userRecipeService.likedRecipe(userId,page));
    }

}
