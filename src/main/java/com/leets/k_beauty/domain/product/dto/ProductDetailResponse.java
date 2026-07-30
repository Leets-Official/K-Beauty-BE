package com.leets.k_beauty.domain.product.dto;

import com.leets.k_beauty.domain.product.entity.Product;
import java.util.List;

public record ProductDetailResponse(
        Long productId,
        String brandName,
        String productName,
        String imageUrl,
        String purchaseUrl,
        Integer price,
        List<IngredientResponse> ingredients
) {
    public static ProductDetailResponse of(Product product, List<IngredientResponse> ingredients) {
        return new ProductDetailResponse(
                product.getId(),
                product.getBrandName(),
                product.getProductName(),
                product.getImageUrl(),
                product.getPurchaseUrl(),
                product.getPrice(),
                ingredients
        );
    }
}
