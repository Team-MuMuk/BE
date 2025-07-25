package com.mumuk.domain.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
public class SearchResponse {

    @Getter
    @AllArgsConstructor
    public static class TrendKeywordListRes {
        private final List<String> trendKeywordList;
        private final LocalDateTime localDateTime;

        public TrendKeywordListRes(List<String> trendKeywordList) {
            this.trendKeywordList = trendKeywordList;
            this.localDateTime = LocalDateTime.now();
        }
    }



}
