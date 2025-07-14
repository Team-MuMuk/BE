package com.mumuk.domain.search.service;

import com.mumuk.domain.search.dto.request.SearchRequest;
import com.mumuk.domain.user.entity.User;
import com.mumuk.domain.user.repository.UserRepository;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.exception.GlobalException;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.exception.AuthException;
import com.mumuk.global.security.jwt.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service

public class RecentSearchServiceImpl implements RecentSearchService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecentSearchServiceImpl(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    // 사용자 조회 로직이 중복되므로 별도 메서드로 분리하여 사용
    private User getUserFromToken(String accessToken) {

        // jwt 토큰이 유효한 토큰인지 확인!
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        // 전화번호로 유효한 사용자인지 확인!
        String phoneNumber = jwtTokenProvider.getPhoneNumberFromToken(accessToken);
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));
        return user;
    }

    @Override
    public void saveRecentSearch(String accessToken, String title) {

        // 사용자가 존재하는지 확인
        User user = getUserFromToken(accessToken);

        // 입력값이 존재하는지 확인
        if (title == null || title.trim().isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_INPUT);
        }

        //redis에 저장할 키
        String key="SearchLog_User:"+user.getId();

        String now= LocalDateTime.now().toString(); // 생성시간 표시용
        // 레디스에 저장할 밸류
        SearchRequest.SavedRecentSearchReq value=new SearchRequest.SavedRecentSearchReq(title,now);

        Long logSize = redisTemplate.opsForList().size(key);
        // 해당 key가 존재하지 않는 경우 발생하는 nullPointerException 방지
        if (logSize==null) {
            throw new GlobalException(ErrorCode.SEARCH_LOG_USER_NOT_FOUND);
        }

        if (logSize==10) {
            redisTemplate.opsForList().rightPop(key);
        } // 최근 검색어는 10개까지! 그 이상은 내보냄

        // 검색어 저장
        redisTemplate.opsForList().leftPush(key,value);

    }

    @Override
    public void deleteRecentSearch(String accessToken, SearchRequest.SavedRecentSearchReq request) {

        // 사용자가 존재하는지 확인
        User user = getUserFromToken(accessToken);

        if (request==null) {
            throw new GlobalException(ErrorCode.INVALID_INPUT);
        }

        //redis에서 조회할 key
        String key="SearchLog_User:"+user.getId();

        //redis에 저장된 value 값과 비교할 객체에 들어갈 매개변수들
        String title=request.getTitle();
        String createdAt=request.getCreatedAt();

        // redis에서 비교할 value
        SearchRequest.SavedRecentSearchReq value=new SearchRequest.SavedRecentSearchReq(title, createdAt);

        Long logSize= redisTemplate.opsForList().size(key);

        if (logSize==null) {
            throw new GlobalException(ErrorCode.SEARCH_LOG_USER_NOT_FOUND);
        }
        // 최근 검색 키워드가 존재하지 않는다면,
        else if (logSize==0) {
            throw new GlobalException(ErrorCode.SEARCH_LOG_NOT_FOUND);
        }

        // 삭제 성공 실패 여부 확인, count가 0이면 실패, 1이면 성공, n개면 n개 삭제했다는 뜻
        long count= redisTemplate.opsForList().remove(key,1,value);

        if (count==0) {
            throw new GlobalException(ErrorCode.KEYWORD_NOT_FOUND);
        }

    }

    @Override
    public List<Object> getRecentSearch(String accessToken) {

        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new AuthException(ErrorCode.JWT_INVALID_TOKEN);
        }

        User user = getUserFromToken(accessToken);

        String key="SearchLog_User:"+user.getId();

        Long logSize= redisTemplate.opsForList().size(key);

        if (logSize==null) {
            throw new GlobalException(ErrorCode.SEARCH_LOG_USER_NOT_FOUND);
        }
        else if (logSize==0) {
            throw new GlobalException(ErrorCode.SEARCH_LOG_NOT_FOUND);
        }

        return redisTemplate.opsForList().range(key,0,9);
    }
}
