package com.mumuk.global.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
public class SmsUtil {

    @Value("${colsms.api.key}")
    private String apiKey;

    @Value("${colsms.api.secret}")
    private String apiSecretKey;

    @Value("${colsms.api.sender}")
    private String senderNumber;

    private DefaultMessageService messageService;

    @PostConstruct
    private void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(
                apiKey,
                apiSecretKey,
                "https://api.coolsms.co.kr" // 고정된 base URL
        );
    }

    public SingleMessageSentResponse sendOne(String to, String messageContent) {
        try{
            Message message = new Message();
            message.setFrom(senderNumber);
            message.setTo(to);
            message.setText(messageContent);

            return this.messageService.sendOne(new SingleMessageSendingRequest(message));
        }catch (Exception e) {
            log.error("SMS 발송 실패: to={}, message={}", to, messageContent, e);
            throw new RuntimeException("SMS 발송에 실패했습니다.", e);
        }
    }
}
