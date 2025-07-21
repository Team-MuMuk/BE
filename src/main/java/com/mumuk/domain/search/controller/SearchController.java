package com.mumuk.domain.search.controller;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.search.dto.request.SearchRequest;
import com.mumuk.domain.search.service.AutocompleteService;
import com.mumuk.domain.search.service.RecentSearchService;
import com.mumuk.domain.search.service.SearchService;
import com.mumuk.domain.search.service.TrendSearchService;
import com.mumuk.domain.user.entity.User;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final AutocompleteService autocompleteService;
    private final RecentSearchService recentSearchService;
    private final TrendSearchService trendSearchService;
    private final SearchService searchService;

    public SearchController(AutocompleteService autocompleteService, RecentSearchService recentSearchService, TrendSearchService trendSearchService, SearchService searchService) {
        this.autocompleteService = autocompleteService;
        this.recentSearchService = recentSearchService;
        this.trendSearchService = trendSearchService;
        this.searchService = searchService;
    }

    @Operation(summary = "레시피 검색결과 목록 조회")
    @GetMapping("/show-resultList")
    public Response<List<RecipeResponse.SimpleRes>> showResultList( @AuthUser Long userId,  @RequestParam String keyword) {

        recentSearchService.saveRecentSearch(userId, keyword);
        List<RecipeResponse.SimpleRes> resultList= searchService.SearchRecipeList(keyword);
        return Response.ok(ResultCode.SEARCH_RECIPE_OK, resultList);
    }

    @Operation(summary = "레시피 검색결과 세부 조회")
    @GetMapping("/show-detailResult/{recipeId}")
    public Response<RecipeResponse.DetailRes> showDetailResult(@RequestParam Long recipeId) {
        RecipeResponse.DetailRes detailResult= searchService.SearchDetailRecipe(recipeId);
        return Response.ok(ResultCode.SEARCH_DETAILRECIPE_OK,detailResult);
    }

    @Operation(summary = "레시피 자동완성 기능")
    @GetMapping("/autocomplete")
    public Response<List<String>> getAutocompleteSuggestions(@RequestParam String userInput) {

        List<String> suggestions = autocompleteService.getAutocompleteSuggestions(userInput);
        return Response.ok(ResultCode.SEARCH_AUTOCOMPLETE_OK, suggestions);
    }

    @Operation(summary = "최근 검색어 저장")
    @PostMapping("/recentsearches/save")
    public Response<Object> saveRecentSearch(@AuthUser Long userId, @RequestParam String keyword){

        recentSearchService.saveRecentSearch(userId, keyword);
        return Response.ok(ResultCode.SEARCH_SAVE_RECENTSEARCHES_OK);
    }

    @Operation(summary = "최근 검색어 삭제")
    @DeleteMapping("/recentsearches/delete")
    public Response<Object> deleteRecentSearch(@AuthUser Long userId, @RequestBody @Valid SearchRequest.SavedRecentSearchReq request){

        recentSearchService.deleteRecentSearch(userId, request);
        return Response.ok(ResultCode.SEARCH_DELETE_RECENTSEARCHES_OK);
    }

    @Operation(summary = "최근 검색어 조회")
    @GetMapping("/recentsearches/get")
    public Response<List<Object>> getRecentSearch(@AuthUser Long userId){

        List<Object> recentSearches= recentSearchService.getRecentSearch(userId);
        return Response.ok(ResultCode.SEARCH_GET_RECENTSEARCHES_OK, recentSearches);
    }

    @Operation(summary = "인기 검색어 조회")
    @GetMapping("/trend")
    public Response<List<String>> getTrend(){
        List<String> trendKeywords=trendSearchService.getTrendKeyword();
        return Response.ok(ResultCode.TRENDKEYWORDS_OK ,trendKeywords);
    }
}
