package com.leets.k_beauty.domain.product.dto;

public record ProductPriceUpdateTarget(
        Long productId,
        String brandName,
        String productName,
        String purchaseUrl
) {
}
