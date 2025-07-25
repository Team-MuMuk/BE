package com.mumuk.domain.search.service;

import com.mumuk.domain.search.dto.response.SearchResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@EnableScheduling
public class TrendSearchServiceImpl implements TrendSearchService {

    // 1시간 단위로 스케줄링!! 스케줄링 어노테이션이 있음

    private final String KEY= "trend_keywords";
    private final RedisTemplate<String, String> redisTemplate;

    public TrendSearchServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void increaseKeywordCount(String keyword) {
        redisTemplate.opsForZSet().incrementScore(KEY,keyword,1);
    }

    @Override
    @Scheduled(cron = "0 0 * * * *")
    public SearchResponse.TrendKeywordListRes getTrendKeyword() {
        Set<String> trendKeywordSet = redisTemplate.opsForZSet().reverseRange(KEY,0,9);
        // 순서 보장 위해 리스트로 한 번 변환
        List<String> trendKeywordList = new ArrayList<>(trendKeywordSet);

        return new SearchResponse.TrendKeywordListRes(trendKeywordList);
    }
}