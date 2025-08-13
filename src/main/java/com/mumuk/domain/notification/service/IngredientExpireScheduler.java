package com.mumuk.domain.notification.service;

import com.mumuk.domain.ingredient.entity.DdayFcmSetting;
import com.mumuk.domain.ingredient.entity.Ingredient;
import com.mumuk.domain.ingredient.entity.IngredientNotification;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        for (User user : users) {
            List<Ingredient> ingredients = ingredientRepository.findAllByUser(user);

            if (ingredients.isEmpty()) continue;


            for (Ingredient ingredient : ingredients) {

                List<IngredientNotification> alarmSettings = ingredient.getDaySettings();

                if (ingredient.getDaySettings() == null || ingredient.getDaySettings().isEmpty()) {
                    continue;
                }

                if (alarmSettings == null || alarmSettings.isEmpty() ||
                        alarmSettings.stream().allMatch(s -> s.getDdayFcmSetting() == DdayFcmSetting.NONE)) {
                    continue;
                }

                long daysLeft = ChronoUnit.DAYS.between(today, ingredient.getExpireDate());


                if (!shouldNotify(ingredient.getDaySettings(), daysLeft)) {
                    continue;
                }
                //제료등록 생성시각 조정
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String createdDate = ingredient.getCreatedAt().format(formatter);

                String title = "유통기한 알림";
                String body = String.format("'%s'에 등록한 '%s'의 유통기한이 %d일 남았어요!",createdDate, ingredient.getName(), daysLeft);

                boolean sent = fcmMessageService.sendFcmMessage(user.getId(), title, body);
                // 알림설정한 기간 리스트를 string 으로 출력
                String settingsText = alarmSettings.stream()
                        .map(s -> s.getDdayFcmSetting().name())
                        .collect(java.util.stream.Collectors.joining(", "));

                if (sent) {
                    log.info("✅ 알림 전송 완료: 재료={}, 남은일수={}, 설정={}",
                            ingredient.getName(), daysLeft, settingsText);
                } else {
                    log.info("⚠️ 알림 전송 실패/스킵: 재료={}, 남은일수={}, 설정={}",
                            ingredient.getName(), daysLeft, settingsText);
                }
            }
        }
    }

    private boolean shouldNotify(List <IngredientNotification> daySettingList , long daysLeft){
        return daySettingList != null &&
                daySettingList.stream()
                        .anyMatch(setting -> setting.getDdayFcmSetting().getDaysBefore() == daysLeft);
    }
}
