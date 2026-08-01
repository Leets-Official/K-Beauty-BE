package com.leets.k_beauty.domain.product.dto;

public record ProductEnrichmentSeedData(
        String brandName,
        String productName,
        String naverSearchQuery,
        String imageUrl,
        String purchaseUrl,
        Integer price
) {
}
