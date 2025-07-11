package com.mumuk.domain.search.service;

import java.util.List;

public interface AutocompleteService {
    List<String> getAutocompleteSuggestions(String userInput);
}

