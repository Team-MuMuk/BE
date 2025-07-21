package com.mumuk.domain.user.service;

import com.mumuk.domain.user.dto.request.AuthRequest;
import com.mumuk.domain.user.dto.request.MypageRequest;
import com.mumuk.domain.user.dto.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


public interface MypageService {


    UserResponse.ProfileInfoDTO profileInfo(Long userId);
    void editProfile(MypageRequest.EditProfileReq request, String accessToken);
    UserResponse.LikedRecipeListDTO likedRecipe(Long userId, Integer page);//UserResponse.RecentRecipeDTO recentRecipe(Long userId);
}
