package com.mumuk.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FCMConfig {

    // application.yml 또는 ENV에서 파일 경로를 지정할 수 있도록 추가
    @Value("${fcm.credentials.file:}")
    private String firebaseCredentialsPath;

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("이미 Firebase 초기화 완료");
            return;
        }

        try (InputStream serviceAccount = resolveInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("✅ Firebase Admin SDK 초기화 완료");
        } catch (Exception e) {
            log.error("Firebase Admin SDK 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }

    /**
     * EC2의 절대 경로 우선, 없으면 classpath에서 로드
     */
    private InputStream resolveInputStream() throws IOException {
        // 1) application.yml or ENV에서 지정한 파일 경로 우선
        if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isBlank()) {
            File file = new File(firebaseCredentialsPath);
            if (file.exists() && file.isFile()) {
                log.info("Firebase 자격증명: 외부 경로 사용 ({})", file.getAbsolutePath());
                return new FileInputStream(file);
            } else {
                log.warn("지정한 Firebase 자격증명 파일이 존재하지 않습니다: {}", firebaseCredentialsPath);
            }
        }

        // 2) classpath fallback
        InputStream cp = getClass().getClassLoader().getResourceAsStream("secrets/firebase-admin-sdk.json");
        if (cp != null) {
            log.info("Firebase 자격증명: classpath(secrets/firebase-admin-sdk.json) 사용");
            return cp;
        }

        throw new FileNotFoundException("Firebase 자격증명 파일을 찾을 수 없습니다 (경로/클래스패스 둘 다 없음)");
    }
}
