package com.leets.k_beauty.domain.recommendation.dto;

import jakarta.validation.constraints.NotNull;

public record CandidateSelectRequest(
        @NotNull(message = "교체할 상품 ID가 필요합니다.")
        Long productId
) {
}
