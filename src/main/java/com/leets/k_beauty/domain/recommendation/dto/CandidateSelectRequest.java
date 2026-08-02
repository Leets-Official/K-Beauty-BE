package com.leets.k_beauty.domain.recommendation.dto;

import jakarta.validation.constraints.NotNull;

public record CandidateSelectRequest(
        @NotNull Long productId
) {
}
