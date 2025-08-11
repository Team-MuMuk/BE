package com.mumuk.domain.search.controller;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.search.dto.request.SearchRequest;
import com.mumuk.domain.search.dto.response.SearchResponse;
import com.mumuk.domain.search.service.*;
import com.mumuk.domain.user.dto.response.UserRecipeResponse;
import com.mumuk.domain.user.entity.User;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import com.mumuk.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "레시피 검색 및 추천/최근/인기 검색어 관련")
public class SearchController {

    private final AutocompleteService autocompleteService;
    private final RecentSearchService recentSearchService;
    private final TrendSearchService trendSearchService;
    private final RecommendedRecipeService recommendedRecipeService;
    private final SearchService searchService;


    public SearchController(AutocompleteService autocompleteService, RecentSearchService recentSearchService, TrendSearchService trendSearchService, SearchService searchService, RecommendedRecipeService recommendedRecipeService) {
        this.autocompleteService = autocompleteService;
        this.recentSearchService = recentSearchService;
        this.trendSearchService = trendSearchService;
        this.searchService = searchService;
        this.recommendedRecipeService = recommendedRecipeService;
    }

    @Operation(summary = "레시피 검색결과 목록 조회")
    @GetMapping("/search")
    public Response<List<UserRecipeResponse.RecipeSummaryDTO>> showResultList(@AuthUser Long userId, @RequestParam String keyword) {
        List<UserRecipeResponse.RecipeSummaryDTO> resultList= searchService.SearchRecipeList(userId, keyword);
        return Response.ok(ResultCode.SEARCH_RECIPE_OK, resultList);
    }

    @Operation(summary = "레시피 검색결과 세부 조회" ,description="사용 x, user-recipe 컨트롤러의 레시피 상세 조회로 통합")
    @GetMapping("/recipes/{recipeId}")
    public Response<RecipeResponse.DetailRes> showDetailResult(@PathVariable Long recipeId) {
        RecipeResponse.DetailRes detailResult= searchService.SearchDetailRecipe(recipeId);
        return Response.ok(ResultCode.SEARCH_DETAILRECIPE_OK,detailResult);
    }

    @Operation(summary ="추천 검색어 조회", description = "가장 최근 검색한 레시피와 같은 카테고리의 레시피 6개 랜덤 제공")
    @GetMapping("/recommended-keywords")
    public Response<List<String>> getRecommendedKeywords(@AuthUser Long userId) {
        List<String> recommendedKeywordList=recommendedRecipeService.getRecommendedRecipeList(userId);
        return Response.ok(ResultCode.SEARCH_GET_RECOMMENDED_KEYWORDS_OK,recommendedKeywordList);
    }

    @Operation(summary = "레시피 자동완성 기능")
    @GetMapping("/recipes/autocomplete")
    public Response<List<String>> getAutocompleteSuggestions(@RequestParam String userInput) {

        List<String> suggestions = autocompleteService.getAutocompleteSuggestions(userInput);
        return Response.ok(ResultCode.SEARCH_AUTOCOMPLETE_OK, suggestions);
    }

    @Operation(summary = "최근 검색어 저장")
    @PostMapping("/recent-searches")
    public Response<Object> saveRecentSearch(@AuthUser Long userId, @RequestParam String keyword){

        recentSearchService.saveRecentSearch(userId, keyword);
        return Response.ok(ResultCode.SEARCH_SAVE_RECENTSEARCHES_OK);
    }

    @Operation(summary = "최근 검색어 삭제")
    @DeleteMapping("/recent-searches")
    public Response<Object> deleteRecentSearch(@AuthUser Long userId, @RequestBody @Valid SearchRequest.SavedRecentSearchReq request){

        recentSearchService.deleteRecentSearch(userId, request);
        return Response.ok(ResultCode.SEARCH_DELETE_RECENTSEARCHES_OK);
    }

    @Operation(summary = "최근 검색어 조회")
    @GetMapping("/recent-searches")
    public Response<List<Object>> getRecentSearch(@AuthUser Long userId){

        List<Object> recentSearches= recentSearchService.getRecentSearch(userId);
        return Response.ok(ResultCode.SEARCH_GET_RECENTSEARCHES_OK, recentSearches);
    }

    @Operation(summary = "인기 검색어 조회")
    @GetMapping("/trends")
    public Response<SearchResponse.TrendKeywordListRes> getTrend(){
        SearchResponse.TrendKeywordListRes trendKeywords=trendSearchService.getTrendKeyword();
        return Response.ok(ResultCode.SEARCH_GET_TRENDKEYWORDS_OK ,trendKeywords);
    }
}
