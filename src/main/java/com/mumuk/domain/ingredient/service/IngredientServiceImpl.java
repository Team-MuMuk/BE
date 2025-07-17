package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.converter.IngredientConverter;
import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.repository.IngredientRepository;
import com.mumuk.domain.user.converter.MypageConverter;
import com.mumuk.domain.user.dto.response.UserResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.domain.user.service.MypageService;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;
import com.mumuk.global.security.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientConverter ingredientConverter;
    private final UserRepository userRepository;
    private final MypageService mypageService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    @Override
    public UserResponse.ProfileInfoDTO profileInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        return MypageConverter.toProfileInfoDTO(user);
    }

    public void registerIngredient(IngredientRegisterRequest dto,String accessToken) {
        //유통기한 날짜 과거날짜 입력시 예외처리
        LocalDate now = LocalDate.now();
        if (dto.getExpireDate().isBefore(now)) {
           throw new GlobalException(ErrorCode.INVALID_EXPIREDATE);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken); // userId 추출

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        Ingredient ingredient = ingredientConverter.toRegister(dto,user);
        ingredientRepository.save(ingredient);
    }
}