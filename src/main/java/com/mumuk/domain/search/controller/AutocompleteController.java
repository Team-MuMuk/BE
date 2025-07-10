package com.mumuk.domain.search.controller;

import com.mumuk.domain.search.service.AutocompleteService;
import com.mumuk.global.apiPayload.code.ErrorCode;
import com.mumuk.global.apiPayload.code.ResultCode;
import com.mumuk.global.apiPayload.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

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
}
