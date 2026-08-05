package com.leets.k_beauty.domain.product.dto;

public record ProductPriceUpdateResult(
        int totalCount,
        int updatedCount,
        int skippedCount,
        int failedCount
) {
}
