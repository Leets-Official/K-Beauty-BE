package com.leets.k_beauty.domain.recommendation.dto;

import com.leets.k_beauty.domain.recommendation.enums.StepRole;

import java.util.List;

public record RecommendationStepResponse(
        Long stepId,
        int step,
        StepRole role,
        RecommendationCandidateResponse selected,       // 현재 선택된 메인 제품
        List<RecommendationCandidateResponse> others    // 다른 후보 2개
) {
}
