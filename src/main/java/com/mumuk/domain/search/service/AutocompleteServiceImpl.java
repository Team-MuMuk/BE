package com.mumuk.domain.search.service;

import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AutocompleteServiceImpl implements AutocompleteService {

    private static final String ZSET_KEY = "autocomplete";
    private final RedisTemplate<String, String> redisTemplate;

    public AutocompleteServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /*
    String, String으로 받는 이유
     key-value에서 key는 autocomplete이고, value는 각 레시피 이름들임
     -> 이건 sorted-set이 아니라 그냥 범용 객체임!!
    */
    @Override
    public List<String> getAutocompleteSuggestions(String userInput) {
        ZSetOperations<String, String> zSetOperations=redisTemplate.opsForZSet();

        if (userInput == null || userInput.trim().isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_INPUT);
        }

        // 사용자가 입력한 단어로 시작되는 저장단어 검색
        // ufff0은 검색 범위를 지정하기 위한 단어임. 연어를 검색했을 때, 연어, 연어+ 회, 연어+ 구이, 연어...+ufff0 까지 검색할 수 있게 하는 역할
        Range<String> range = Range.closed(userInput, userInput + "\ufff0");

        // 정렬 결과를 5개만 가져옴
        Set<String> results=zSetOperations.rangeByLex(ZSET_KEY, range, Limit.limit().count(5));

        if (results.isEmpty()) {
            throw new GlobalException(ErrorCode.KEYWORD_NOT_FOUND);
        }

        return new ArrayList<>(results);

    }
}
