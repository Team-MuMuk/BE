package com.mumuk.domain.recipe.service;

public interface RecipeBlogImageAsyncService {
    void fetchAndCacheImage(String blogUrl);
    String getCachedImage(String blogUrl);
}
