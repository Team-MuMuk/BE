package com.mumuk.domain.ingredient.converter;

import com.mumuk.domain.ingredient.dto.request.IngredientRegisterRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientRegisterResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IngredientConverter {

    public Ingredient toRegister(IngredientRegisterRequest dto) {
        System.out.println("✅ expireDate: " + dto.getExpireDate()); // null 여부 확인
        System.out.println("✅ Name: " + dto.getName()); // null 여부 확인

        return Ingredient.builder()
                .name(dto.getName())
                .expireDate(dto.getExpireDate())
                .daySetting((dto.getDaySetting()))
                .build();
    }
}