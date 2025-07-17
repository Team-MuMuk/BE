package com.mumuk.global.config;

import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String ZSET_KEY="autocomplete";

    /*
    DB에 있는 데이터를 redis에 추가하는 코드는 추후에 작성하겠습니다..

     */
    @Override
    public void run(String... args) throws Exception {

        ZSetOperations<String,String> zSetOperations=redisTemplate.opsForZSet();

        String [] recipes={
                "연어", "연어회","연어장","연어초밥", "연어스테이크",
                "킬바사", "킬바사 소세지","킬바사 부대찌개"
        };

        for (String recipe : recipes) {
            zSetOperations.add(ZSET_KEY,recipe,0);
        }
    }
}