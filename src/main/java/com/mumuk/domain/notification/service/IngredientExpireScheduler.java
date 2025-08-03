package com.mumuk.domain.notification.service;

import com.mumuk.domain.ingredient.entity.DdayFcmSetting;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.repository.IngredientRepository;
import com.mumuk.domain.notification.entity.Fcm;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component

public class IngredientExpireScheduler {

    private final IngredientRepository ingredientRepository;
    private final FcmMessageService fcmMessageService;
    private final UserRepository userRepository;

    public IngredientExpireScheduler(IngredientRepository ingredientRepository, FcmMessageService fcmMessageService, UserRepository userRepository) {
        this.ingredientRepository = ingredientRepository;
        this.fcmMessageService = fcmMessageService;
        this.userRepository = userRepository;
    }

    //@Scheduled(cron = "*/10 * * * * ?")//테스트용
    @Scheduled(cron = "0 0 9 * * ?") // 매일 오전 9시
    public void sendExpiryNotifications() {
        List<User> users = userRepository.findByFcmAgreed(true);

        LocalDate today = LocalDate.now();

        for (User user : users) {
            List<Ingredient> ingredients = ingredientRepository.findAllByUser(user);

            if (ingredients.isEmpty()) continue;


            for (Ingredient ingredient : ingredients) {
                if (ingredient.getExpireDate() == null || ingredient.getDaySetting() == null) {
                    continue;
                }

                long daysLeft = ChronoUnit.DAYS.between(today, ingredient.getExpireDate());


                if (!shouldNotify(ingredient.getDaySetting(), daysLeft)) {
                    continue;
                }

                String title = "유통기한 알림";
                String body = String.format("'%s'의 유통기한이 %d일 남았어요!", ingredient.getName(), daysLeft);

                boolean sent = fcmMessageService.sendFcmMessage(user.getId(), title, body);

                if (sent) {
                    log.info("✅ 알림 전송 완료: 재료={}, 남은일수={}, 설정={}",
                            ingredient.getName(), daysLeft, ingredient.getDaySetting());
                } else {
                    log.info("⚠️ 알림 전송 실패/스킵: 재료={}, 남은일수={}, 설정={}",
                            ingredient.getName(), daysLeft, ingredient.getDaySetting());
                }
            }
        }
    }

    private boolean shouldNotify(List <DdayFcmSetting> daySettingList ,long daysLeft){
        return daySettingList != null &&
                daySettingList.stream()
                        .anyMatch(setting -> setting.getDaysBefore() == daysLeft);
    }
}
