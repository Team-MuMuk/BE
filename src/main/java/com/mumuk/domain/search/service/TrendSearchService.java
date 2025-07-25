package com.mumuk.domain.search.service;

import com.mumuk.domain.search.dto.response.SearchResponse;

import java.util.List;

public interface TrendSearchService {
    public void increaseKeywordCount(String keyword);
    public SearchResponse.TrendKeywordListRes getTrendKeyword();

}
