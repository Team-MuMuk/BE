package com.mumuk.domain.recipe.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
public class RecipeBlogImageAsyncServiceImpl implements RecipeBlogImageAsyncService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration IMAGE_CACHE_TTL = Duration.ofDays(3);    // 3일 동안 이미지 캐시

    public RecipeBlogImageAsyncServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Async
    public void fetchAndCacheImage(String blogUrl) {
        try {
            String mobileUrl = blogUrl.replace("https://blog.naver.com", "https://m.blog.naver.com");
            Document doc = Jsoup.connect(mobileUrl).timeout(5000).get();
            String ogImage = doc.select("meta[property=og:image]").attr("content");

            if (ogImage != null && !ogImage.isBlank()) {
                String key = toRedisKey(blogUrl);
                redisTemplate.opsForValue().set(key, ogImage, IMAGE_CACHE_TTL);
            }
        } catch (Exception e) {
            // 로그 정도만 찍고 실패해도 넘어감
            log.warn("이미지 크롤링 실패: {}, 오류: {}", blogUrl, e.getMessage());
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
    }
}
