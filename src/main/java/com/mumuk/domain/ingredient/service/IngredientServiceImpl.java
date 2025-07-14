package com.mumuk.domain.ingredient.service;

import com.mumuk.domain.ingredient.converter.IngredientConverter;
import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientRegisterResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.repository.IngredientRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;
import com.mumuk.global.security.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientConverter ingredientConverter;
    private final UserRepository userRepository;


    public void registerIngredient(IngredientRegisterRequest dto) {
        //유통기한 날짜 과거날짜 입력시 예외처리
        LocalDate now = LocalDate.now();
        if (dto.getExpireDate().isBefore(now)) {
           throw new GlobalException(ErrorCode.INVALID_EXPIREDATE);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        Ingredient ingredient = ingredientConverter.toRegister(dto, user);
        ingredientRepository.save(ingredient);
    }
}