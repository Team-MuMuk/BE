package com.mumuk.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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
        try (InputStream serviceAccount = new FileInputStream(firebaseAdminSdkPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK 초기화 완료");
            }
            else {
            System.out.println("이미 Firebase 초기화 완료");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
