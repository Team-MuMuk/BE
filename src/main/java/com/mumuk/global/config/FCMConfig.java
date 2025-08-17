package com.mumuk.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FCMConfig {

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("이미 Firebase 초기화 완료");
            return;
        }

        try (InputStream serviceAccount =
                     getClass().getClassLoader().getResourceAsStream("fcm/firebase-sdk.json")) {
            if (serviceAccount == null) {
                throw new IllegalStateException("Firebase Admin SDK JSON이 classpath에 없습니다.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("✅ Firebase Admin SDK 초기화 완료");
        } catch (IOException e) {
            log.error("Firebase Admin SDK 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }
}
