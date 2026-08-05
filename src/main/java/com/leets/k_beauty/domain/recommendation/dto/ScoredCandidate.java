package com.leets.k_beauty.domain.recommendation.dto;

import com.leets.k_beauty.domain.product.dto.ProductCandidate;

/**
 * 점수와 추천 이유가 포함된 후보 상품.
 * RecommendationScoringService → RecommendationService 전달용.
 */
public record ScoredCandidate(
        ProductCandidate product,
        int matchScore,
        String reasonShort
) {}
