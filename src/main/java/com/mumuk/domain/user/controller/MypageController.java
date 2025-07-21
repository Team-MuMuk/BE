package com.mumuk.domain.user.controller;

import com.mumuk.domain.user.converter.MypageConverter;
import com.mumuk.domain.user.converter.OAuthConverter;
import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.request.MypageRequest;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.service.AuthService;
import com.mumuk.domain.user.service.MypageService;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{id}")
@Slf4j
public class MypageController {


    private final MypageService mypageService;

    public MypageController(MypageService mypageService) {

        this.mypageService = mypageService;
    }

    // 프로필 정보 조회
    @Operation(summary = "프로필 조회", description = "사용자의 프로필 정보를 조회합니다.")
    @GetMapping
    public Response<UserResponse.ProfileInfoDTO> profileInfo(@AuthUser Long userId) {
        return Response.ok(mypageService.profileInfo(userId));
    }

    // 프로필 정보 수정
    @Operation(summary = "프로필 수정", description = "사용자의 프로필 정보를 수정합니다.")
    @PatchMapping
    public Response<String> editProfile(@Valid @RequestBody MypageRequest.EditProfileReq req, HttpServletRequest request ) {
        String accessToken = request.getHeader("Authorization");
        mypageService.editProfile(req, accessToken);
        return Response.ok(ResultCode.EDIT_PROFILE_OK, "프로필이 수정되었습니다.");
    }

    //찜한 레시피 조회
    @Operation(summary = "찜한 레시피 조회", description = "사용자가 찜한 레시피를 조회합니다.")
    @GetMapping("/liked-recipe")
    public Response<UserResponse.LikedRecipeListDTO> likedRecipe(@AuthUser Long userId, @RequestParam(defaultValue = "0") int page) {
        return Response.ok(mypageService.likedRecipe(userId,page));
    }

    //최근 레시피 조회
    /*@Operation(summary = "최근 레시피 조회", description = "사용자가 최근에 본 레시피를 조회합니다.")
    @GetMapping("/recent-recipe")
    public Response<UserResponse.RecentRecipeDTO> recentRecipe(@AuthUser Long userId) {
        return Response.ok(mypageService.recentRecipe(userId));
    }*/



}
