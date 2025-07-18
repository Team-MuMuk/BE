package com.mumuk.domain.ingredient.service;


import com.mumuk.domain.ingredient.converter.IngredientConverter;
import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.repository.IngredientRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.security.exception.AuthException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientConverter ingredientConverter;
    private final UserRepository userRepository;

    public IngredientServiceImpl(IngredientRepository ingredientRepository, IngredientConverter ingredientConverter,UserRepository userRepository) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientConverter = ingredientConverter;
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public void registerIngredient(IngredientRequest.RegisterReq req, Long userId) {
        //유통기한 날짜 과거날짜 입력시 예외처리
        LocalDate now = LocalDate.now();
        if (req.getExpireDate().isBefore(now)) {
           throw new BusinessException(ErrorCode.INVALID_EXPIREDATE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        Ingredient ingredient = ingredientConverter.toRegister(req,user);
        ingredientRepository.save(ingredient);

    }
}