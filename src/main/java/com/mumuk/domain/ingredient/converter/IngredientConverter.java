package com.mumuk.domain.ingredient.converter;

import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientRegisterResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.user.entity.User;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IngredientConverter {

    public Ingredient toRegister(IngredientRegisterRequest dto, User user) {

        return Ingredient.builder()
                .name(dto.getName())
                .expireDate(dto.getExpireDate())
                .daySetting((dto.getDaySetting()))
                .user(user)
                .build();
    }
}