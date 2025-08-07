package com.mumuk.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FCMConfig {

    @Value("${firebase.admin.sdk.path}")
    private String firebaseAdminSdkPath;

    @PostConstruct
    public void initialize() {

        if (firebaseAdminSdkPath == null || firebaseAdminSdkPath.trim().isEmpty()) {
            log.error("Firebase Admin SDK 파일 경로가 설정되지 않았습니다");
                throw new IllegalStateException("Firebase Admin SDK 파일 경로가 필요합니다");
        }

        File credentialFile = new File(firebaseAdminSdkPath);
        if (!credentialFile.exists()) {
            log.error("Firebase Admin SDK 파일이 존재하지 않습니다: {}", firebaseAdminSdkPath);
            throw new IllegalStateException("Firebase Admin SDK 파일을 찾을 수 없습니다");
        }

        try (InputStream serviceAccount = new FileInputStream(firebaseAdminSdkPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK 초기화 완료");
            }
            else {
                log.info("이미 Firebase 초기화 완료");
            }

            } catch (IOException e) {
                log.error("Firebase Admin SDK 초기화 실패: 인증 파일을 읽을 수 없습니다", e);
                throw new RuntimeException("Firebase 초기화 실패", e);
            } catch (Exception e) {
                log.error("Firebase Admin SDK 초기화 중 예상치 못한 오류 발생", e);
                throw new RuntimeException("Firebase 초기화 실패", e);
            }
    }
}
