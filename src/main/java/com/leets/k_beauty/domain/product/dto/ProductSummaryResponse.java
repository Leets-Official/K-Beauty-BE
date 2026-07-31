package com.leets.k_beauty.domain.product.dto;

import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.enums.ProductCategory;

public record ProductSummaryResponse(
        Long productId,
        String brandName,
        String productName,
        ProductCategory category,
        String imageUrl,
        Integer price
) {
    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getBrandName(),
                product.getProductName(),
                product.getCategory(),
                product.getImageUrl(),
                product.getPrice()
        );
    }
}
