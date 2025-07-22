package com.mumuk.domain.search.service;

import com.mumuk.domain.search.dto.request.SearchRequest;

import java.util.List;


public interface RecentSearchService {
    public void saveRecentSearch(Long userId, String title);
    // keyword와 현재시각을 입력하여 saveRecentSearchReq 객체 생성, 이를 저장
    // 굳이 객체를 입력받아 연동하기보다 이 방식이 더 빠르게 저장할 수 있을 것이라 생각했음

    public void deleteRecentSearch(Long userId, SearchRequest.SavedRecentSearchReq request);
    // 객체를 입력받아 redis에 저장한 value와 비교, 해당 value를 지우는 로직
    // createdAt을 keyword 만으로 불러올 방법이 없기 때문에 일치하는 객체를 찾는 것이 빠르다 생각했음
    public List<Object> getRecentSearch( Long userId);


}
