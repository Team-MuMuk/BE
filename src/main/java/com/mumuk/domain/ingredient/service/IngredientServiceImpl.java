package com.mumuk.domain.ingredient.service;


import com.mumuk.domain.ingredient.converter.IngredientConverter;
import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.repository.IngredientRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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
    @Override
    @Transactional(readOnly = true)
    public List<IngredientResponse.RetrieveRes> getAllIngredient(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        List<Ingredient> ingredients = ingredientRepository.findAllByUser(user);

        return ingredients.stream()
                .map(ingredientConverter::toRetrieve)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void updateIngredient(Long ingredientId, IngredientRequest.UpdateReq req, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));

        if (!ingredient.getUser().getId().equals(user.getId())) {
            throw new AuthException(ErrorCode.USER_NOT_EQUAL); //사용자의 재료와 재료의 user 비교
        }
        LocalDate today = LocalDate.now();
        if (req.getExpireDate().isBefore(today)) {
            throw new BusinessException(ErrorCode.INVALID_EXPIREDATE);
        }
        ingredient.setName(req.getName());
        ingredient.setExpireDate(req.getExpireDate());
        ingredient.setDaySetting(req.getDaySetting());

        ingredientRepository.save(ingredient);
    }

    @Transactional
    @Override
    public void deleteIngredient(Long ingredientId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));

        if (!ingredient.getUser().getId().equals(user.getId())) {
            throw new AuthException(ErrorCode.USER_NOT_EQUAL); //사용자의 재료와 재료의 user 비교
        }

        ingredientRepository.delete(ingredient);
    }
    @Transactional(readOnly = true)
    @Override
    public List<IngredientResponse.ExpireDateManegeRes> getCloseExpireDateIngredients(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate limitDate = today.plusDays(30);//유통기한 임박재료 기준을 30일로 잡음

        List<Ingredient> ingredients = ingredientRepository
                .findByUserAndExpireDateBetweenOrderByExpireDateAsc(user, today, limitDate);

        return ingredients.stream()
                .map(ingredientConverter::toExpireDate)
                .collect(Collectors.toList());
    }



}