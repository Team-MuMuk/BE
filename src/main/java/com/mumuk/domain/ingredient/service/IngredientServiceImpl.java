package com.mumuk.domain.ingredient.service;


import com.mumuk.domain.ingredient.converter.IngredientConverter;
import com.mumuk.domain.ingredient.dto.request.IngredientRequest;
import com.mumuk.domain.ingredient.dto.response.IngredientResponse;
import com.mumuk.domain.ingredient.entity.DdayFcmSetting;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.entity.IngredientNotification;
import com.mumuk.domain.ingredient.repository.IngredientRepository;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientConverter ingredientConverter;
    private final UserRepository userRepository;

    public IngredientServiceImpl(IngredientRepository ingredientRepository, IngredientConverter ingredientConverter, UserRepository userRepository) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientConverter = ingredientConverter;
        this.userRepository = userRepository;
    }

    @Transactional
    @Override
    public void registerIngredient(IngredientRequest.RegisterReq req, Long userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (req.getExpireDate().isBefore(today)) {
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
            throw new AuthException(ErrorCode.USER_NOT_EQUAL);
        }

        List<IngredientNotification> alarmSetting = req.getDaySetting().stream()
                .map(setting -> new IngredientNotification(ingredient, setting))
                .toList(); //추후 수정가능성이 없으므로 toList로 반환.추후 알림설정한 리스트에대해 수정필요할시 .collect(Collectors.toList());

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        if (req.getExpireDate().isBefore(today)) {
            throw new BusinessException(ErrorCode.INVALID_EXPIREDATE);
        }

        ingredient.getDaySettings().clear();
        ingredient.getDaySettings().addAll(alarmSetting); //알림설정 최신화 (NONE 포함조건 불필요)


        ingredient.setName(req.getName());
        ingredient.setExpireDate(req.getExpireDate());
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

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate limitDate = today.plusDays(7); //유통기한 임박재료 기준 7일

        List<Ingredient> ingredients = ingredientRepository
                .findByUserAndExpireDateBetweenOrderByExpireDateAsc(user, today, limitDate);

        return ingredients.stream()
                .map(ingredientConverter::toExpireDate)
                .collect(Collectors.toList());
    }
}
