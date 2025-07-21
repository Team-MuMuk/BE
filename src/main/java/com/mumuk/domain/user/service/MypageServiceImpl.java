package com.mumuk.domain.user.service;


import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.user.converter.MypageConverter;
import com.mumuk.domain.user.converter.TokenResponseConverter;
import com.mumuk.domain.user.dto.request.MypageRequest;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.entity.UserRecipe;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import com.mumuk.global.util.SmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.mumuk.domain.user.converter.MypageConverter.toLikedRecipeListDTO;

@Slf4j
@Service
public class MypageServiceImpl implements MypageService {

    private final UserRepository userRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final JwtTokenProvider jwtTokenProvider;


    public MypageServiceImpl(UserRepository userRepository, UserRecipeRepository userRecipeRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.jwtTokenProvider = jwtTokenProvider;

    }

    @Override
    @Transactional
    public UserResponse.ProfileInfoDTO profileInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        return MypageConverter.toProfileInfoDTO(user);
    }

    @Override
    @Transactional
    public void editProfile(MypageRequest.EditProfileReq request,String accessToken) {
        String phoneNumber = jwtTokenProvider.getPhoneNumberFromToken(accessToken);
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        user.setName(request.getName());
        user.setNickName(request.getNickName());
        user.setProfileImage(request.getProfileImage());
        user.setStatusMessage(request.getStatusMessage());
        userRepository.save(user);

    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse.LikedRecipeListDTO likedRecipe(Long userId, Integer page) {
        //사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        //사용자가 찜한 레시피를 조회
        Page<UserRecipe> likedUserRecipes = userRecipeRepository.findByUser_IdAndLikedIsTrue(user.getId(),PageRequest.of(page, 6));
        //Converter: Page<UserRecipe> -> LikedRecipeListDTO
        UserResponse.LikedRecipeListDTO likedRecipeListDTO = MypageConverter.toLikedRecipeListDTO(likedUserRecipes);
        return likedRecipeListDTO;
    }

    /*@Override
    @Transactional(readOnly = true)
    public UserResponse.RecentRecipeDTO recentRecipe(Long userId) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // 2. 최근 조회한 레시피를 조회하기 위해 Pageable 객체를 생성합니다.
        // 한 페이지에 8개의 결과를 가져오고, 첫 번째 페이지(인덱스 0)부터 시작합니다.
        Pageable pageable = PageRequest.of(0, 8);

        // 3. 해당 사용자가 조회한 레시피(UserRecipe 엔티티)들을 조회하고, 조회 시간 내림차순으로 정렬하여 8개까지 가져옵니다.
        List<UserRecipe> recentUserRecipes = userRecipeRepository.findByUser_IdAndViewedIsTrueOrderByViewedAtDesc(userId, pageable);

        // 4. UserRecipe 엔티티에서 실제 Recipe 정보를 추출하여 DTO로 변환합니다.
        List<UserResponse.RecipeSummaryDTO> recentRecipes = recentUserRecipes.stream()
                .map(userRecipe -> {
                    Recipe recipe = userRecipe.getRecipe();
                    // 여기에 RecipeSummaryDTO를 구성하는 로직을 작성합니다.
                    return UserResponse.RecipeSummaryDTO.builder()
                            .recipeId(recipe.getId())
                            .recipeName(recipe.getName())
                            .description(recipe.getDescription())
                            // .imageUrl(recipe.getImages().isEmpty() ? null : recipe.getImages().get(0).getImageUrl()) // 첫 번째 이미지 URL
                            .build();
                })
                .collect(Collectors.toList());

        // 5. 최종 RecentRecipeDTO 객체를 생성하여 반환합니다.
        return UserResponse.RecentRecipeDTO.builder()
                .userId(userId)
                .recentRecipes(recentRecipes)
                .build();
    }*/

}
