package com.mumuk.domain.ingredient.converter;

import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class IngredientConverter {

    public Ingredient toRegister(IngredientRequest.RegisterReq req, User user) {
        return Ingredient.builder()
                .name(req.getName())
                .expireDate(req.getExpireDate())
                .user(user)
                .build();
    }

    public IngredientResponse.RetrieveRes toRetrieve(Ingredient ingredient) {
        return new IngredientResponse.RetrieveRes(
                ingredient.getName(),
                ingredient.getExpireDate(),
                ingredient.getCreatedAt());

    }

    public IngredientResponse.ExpireDateManegeRes toExpireDate(Ingredient ingredient) {
        LocalDate today = LocalDate.now();
        long daysLeft = ChronoUnit.DAYS.between(today, ingredient.getExpireDate());
        String dDay = "D-" + daysLeft;
        return new IngredientResponse.ExpireDateManegeRes(
                ingredient.getName(),
                ingredient.getExpireDate(),
                dDay);

    }
}
