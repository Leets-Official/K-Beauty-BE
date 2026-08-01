package com.leets.k_beauty.domain.recommendation.dto;

import com.leets.k_beauty.domain.recommendation.entity.Recommendation;
import com.leets.k_beauty.domain.recommendation.enums.RecommendationStatus;
import com.leets.k_beauty.domain.session.enums.SearchHabit;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long id,
        RecommendationStatus status,
        SearchHabit searchHabit,
        List<RecommendationStepResponse> steps,
        LocalDateTime createdAt
) {
    public static RecommendationResponse of(Recommendation recommendation, List<RecommendationStepResponse> steps) {
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getStatus(),
                recommendation.getSearchHabit(),
                steps,
                recommendation.getCreatedAt()
        );
    }
}
