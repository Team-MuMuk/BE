package com.mumuk.domain.ingredient.converter;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class IngredientConverter {

    public Ingredient toRegister(IngredientRequest.RegisterReq req, User user) {
        return Ingredient.builder()
                .name(req.getName())
                .expireDate(req.getExpireDate())
                .daySetting(req.getDaySetting())
                .user(user)
                .build();
    }

    public IngredientResponse.RetrieveRes toRetrieve(Ingredient ingredient) {
        return new IngredientResponse.RetrieveRes(
                ingredient.getName(),
                ingredient.getExpireDate(),
                ingredient.getCreatedAt());

    }
}