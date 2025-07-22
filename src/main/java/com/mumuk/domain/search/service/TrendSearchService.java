package com.mumuk.domain.search.service;

import java.util.List;

public interface TrendSearchService {
    public void increaseKeywordCount(String keyword);
    public List<String> getTrendKeyword();

}
