package com.mumuk.domain.recipe.controller;

import com.mumuk.domain.recipe.dto.response.RecipeResponse;
import com.mumuk.domain.recipe.service.RecipeRecommendService;
import com.mumuk.global.security.annotation.AuthUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/recipe/recommend")
public class RecipeRecommendController {
    private final RecipeRecommendService recommendService;
    public RecipeRecommendController(RecipeRecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping("/ingredient")
    public ResponseEntity<List<RecipeResponse.DetailRes>> recommendByIngredient(@AuthUser Long userId) {
        List<RecipeResponse.DetailRes> result = recommendService.recommendByIngredient(userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/random")
    public ResponseEntity<List<RecipeResponse.DetailRes>> recommendRandom() {
        List<RecipeResponse.DetailRes> result = recommendService.recommendRandom();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/health")
    public ResponseEntity<List<RecipeResponse.DetailRes>> recommendByHealth(@RequestBody HealthRecommendRequest req) {
        // 실제 구현은 추후 개발 예정
        // List<RecipeResponse.DetailRes> result = recommendService.recommendByHealth(req);
        // return ResponseEntity.ok(result);
        return ResponseEntity.ok(List.of()); // 샘플/주석 처리
    }

    // DTO 예시 (실제 위치/구조는 프로젝트에 맞게 조정)
    public static class HealthRecommendRequest {
        // 성별, 키, 체중, 골격근량, 체지방량, 체지방률, BMI, BMR, 알레르기, 건강목표 등
        // 실제 구현 시 필드 추가
    }
} 