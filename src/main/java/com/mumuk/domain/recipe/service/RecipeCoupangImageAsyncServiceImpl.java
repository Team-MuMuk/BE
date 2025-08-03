package com.mumuk.domain.recipe.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class RecipeCoupangImageAsyncServiceImpl
        //extends RecipeCoupangImageAsyncService
{

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration IMAGE_CACHE_TTL = Duration.ofDays(3);    // 3일 동안 이미지 캐시

    public RecipeCoupangImageAsyncServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

   /* @Override
    @Async
    public void fetchAndCacheImage(String coupangUrl) {
        try {
            String mobileUrl = coupangUrl.replace("https://www.coupang.com", "https://m.coupang.com");
            Document doc = Jsoup.connect(mobileUrl).timeout(5000).get();
            String ogImage = doc.select("meta[property=og:image]").attr("content");

            if (ogImage != null && !ogImage.isBlank()) {
                String key = toRedisKey(coupangUrl);
                redisTemplate.opsForValue().set(key, ogImage, IMAGE_CACHE_TTL);
            }
        } catch (Exception e) {
            // 로그 정도만 찍고 실패해도 넘어감
            log.warn("이미지 크롤링 실패: {}, 오류: {}", coupangUrl, e.getMessage());
        }
    }

    @Override
    public String getCachedImage(String blogUrl) {
        String key = toRedisKey(blogUrl);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    private static String toRedisKey(String blogUrl) {
        return "blog:image:" + DigestUtils.md5DigestAsHex(blogUrl.getBytes(StandardCharsets.UTF_8));
    }*/

}
