package com.leets.k_beauty.domain.recommendation.dto;

import com.leets.k_beauty.domain.product.dto.IngredientInfo;
import com.leets.k_beauty.domain.product.dto.ProductCandidate;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import com.leets.k_beauty.domain.recommendation.entity.RecommendationCandidate;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationCandidateResponse(
        Long candidateId,
        Long productId,
        String brandName,
        String productName,
        ProductCategory category,
        String imageUrl,
        String purchaseUrl,
        Integer price,
        int candidateRank,
        boolean userSelected,
        List<IngredientInfo> topIngredients,  // 주요 성분 칩 최대 3개
        BigDecimal matchScore,
        String reasonShort
) {
    public static RecommendationCandidateResponse of(RecommendationCandidate candidate, ProductCandidate product) {
        List<IngredientInfo> top3 = product.ingredients().stream()
                .limit(3)
                .toList();

        return new RecommendationCandidateResponse(
                candidate.getId(),
                product.productId(),
                product.brandName(),
                product.productName(),
                product.category(),
                product.imageUrl(),
                product.purchaseUrl(),
                product.price(),
                candidate.getCandidateRank(),
                candidate.isUserSelected(),
                top3,
                candidate.getMatchScore(),
                candidate.getReasonShort()
        );
    }
}
