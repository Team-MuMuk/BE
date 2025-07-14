package com.mumuk.domain.search.controller;

import com.mumuk.domain.search.dto.request.SearchRequest;
import com.mumuk.domain.search.service.AutocompleteService;
import com.mumuk.domain.search.service.RecentSearchService;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.DELETE;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final AutocompleteService autocompleteService;
    private final RecentSearchService recentSearchService;

    public SearchController(AutocompleteService autocompleteService, RecentSearchService recentSearchService) {
        this.autocompleteService = autocompleteService;
        this.recentSearchService = recentSearchService;
    }

    @GetMapping("/autocomplete")
    public Response<List<String>> getAutocompleteSuggestions(@RequestParam  String userInput) {

        List<String> suggestions = autocompleteService.getAutocompleteSuggestions(userInput);
        return Response.ok(ResultCode.SEARCH_AUTOCOMPLETE_OK, suggestions);
    }


    @PostMapping("/recentsearches/save")
    public Response<Object> saveRecentSearch(@RequestParam String accessToken, @RequestParam String keyword){

        recentSearchService.saveRecentSearch(accessToken, keyword);
        return Response.ok(ResultCode.SEARCH_SAVE_RECENTSEARCHES_OK);
    }

    @DeleteMapping("/recentsearches/delete")
    public Response<Object> deleteRecentSearch(@RequestParam String accessToken, @RequestBody @Valid SearchRequest.SavedRecentSearchReq request){

        recentSearchService.deleteRecentSearch(accessToken, request);
        return Response.ok(ResultCode.SEARCH_DELETE_RECENTSEARCHES_OK);
    }

    @GetMapping("/recentsearches/get")
    public Response<List<Object>> getRecentSearch(@RequestParam String accessToken){

        List<Object> recentSearches= recentSearchService.getRecentSearch(accessToken);
        return Response.ok(ResultCode.SEARCH_GET_RECENTSEARCHES_OK, recentSearches);
    }
}
