package com.mumuk.global.config;

import com.mumuk.domain.recipe.entity.Recipe;
import com.mumuk.domain.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisAutocompleteInitializer implements CommandLineRunner {

    private static final String ZSET_KEY = "recipetitles";
    private final RedisTemplate<String, String> redisTemplate;
    private final RecipeRepository recipeRepository;

    @Override
    public void run(String... args) throws Exception {
        List<Recipe> allRecipes = recipeRepository.findAll();

        for (Recipe recipe : allRecipes) {
            redisTemplate.opsForZSet().add(ZSET_KEY, recipe.getTitle(), 0);
        }

    }
}
