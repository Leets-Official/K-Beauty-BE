package com.leets.k_beauty.domain.product.dto;

import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import java.util.List;

public record ProductSummaryResponse(
        Long productId,
        String brandName,
        String productName,
        ProductCategory category,
        String imageUrl,
        Integer price,
        List<String> ingredientNames
) {
    public static ProductSummaryResponse of(Product product, List<String> ingredientNames) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getBrandName(),
                product.getProductName(),
                product.getCategory(),
                product.getImageUrl(),
                product.getPrice(),
                ingredientNames
        );
    }
}
