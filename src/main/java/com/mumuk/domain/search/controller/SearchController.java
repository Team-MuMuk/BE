package com.mumuk.domain.search.controller;

import com.mumuk.domain.search.dto.request.SearchRequest;
import com.mumuk.domain.search.service.AutocompleteService;
import com.mumuk.domain.search.service.RecentSearchService;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.DELETE;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final AutocompleteService autocompleteService;
    private final RecentSearchService recentSearchService;

    @GetMapping("/autocomplete")
    public Response<List<String>> getAutocompleteSuggestions(@RequestParam String userInput) {

        if (userInput == null || userInput.trim().isEmpty()) {
            return Response.fail(ErrorCode.INVALID_INPUT,List.of());
        }

        List<String> suggestions = autocompleteService.getAutocompleteSuggestions(userInput);
        if (suggestions.isEmpty()) {
            return Response.fail(ErrorCode.KEYWORD_NOT_FOUND, suggestions);
        } else {
            return Response.ok(ResultCode.SEARCH_AUTOCOMPLETE_OK, suggestions);

        }
    }

    @PostMapping("recentsearches/save")
    public Response<Object> saveRecentSearch(String accessToken, String keyword){
        if (keyword == null || keyword.trim().isEmpty()) {
            return Response.fail(ErrorCode.INVALID_INPUT);
        }

        recentSearchService.saveRecentSearch(accessToken, keyword);

        return Response.ok(ResultCode.SEARCH_SAVE_RECENTSEARCHES_OK);
    }

    @DeleteMapping("recentsearches/delete")
    public Response<Object> deleteRecentSearch(String accessToken, SearchRequest.SavedRecentSearchReq request){
        if (request==null) {
            return Response.fail(ErrorCode.INVALID_INPUT);
        }
        recentSearchService.deleteRecentSearch(accessToken, request);

        return Response.ok(ResultCode.SEARCH_DELETE_RECENTSEARCHES_OK);
    }

    @GetMapping("recentsearches/get")
    public Response<List<Object>> getRecentSearch(String accessToken){

        recentSearchService.getRecentSearch(accessToken);

        return Response.ok(ResultCode.SEARCH_GET_RECENTSEARCHES_OK, recentSearchService.getRecentSearch(accessToken));
    }




}
