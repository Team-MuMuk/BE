package com.mumuk.domain.user.service;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import com.mumuk.domain.search.dto.request.SearchRequest;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRecipeRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;
import com.mumuk.global.security.exception.AuthException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecentRecipeServiceImpl implements RecentRecipeService{

    private final StringRedisTemplate redisTemplate;

    public RecentRecipeServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

    }
    //사용자가 상세 페이지 api를 호출할 때마다 redis에 아래의 데이터를 저장
    //key: user:1:recent_recipes
    //Sorted set - {member: recipeId, score:조회 시간 타임 스탬프}로 저장
    public void addRecentRecipe(Long userId, Long recipeId) {
        String key = "user:" + userId + ":recent_recipes";
        long currentTimestamp = System.currentTimeMillis();

        // ZADD 명령어 실행: recipeId를 member, 현재 시간을 score 로 추가
        redisTemplate.opsForZSet().add(key, String.valueOf(recipeId), (double) currentTimestamp);
        //최대 8개까지 저장
        final long MAX_RECENT_RECIPES = 8;

        // 현재 Sorted Set의 크기를 확인
        Long size = redisTemplate.opsForZSet().size(key);

        // 만약 크기가 제한을 초과했다면, 가장 오래된 항목들을 삭제
        if (size != null && size > MAX_RECENT_RECIPES) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_RECENT_RECIPES - 1);
        }
    }


}
