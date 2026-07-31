package com.leets.k_beauty.domain.product.dto;

import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import java.util.List;

public record ProductCandidate(
        Long productId,
        String brandName,
        String productName,
        ProductCategory category,
        String imageUrl,
        String purchaseUrl,
        Integer price,
        List<IngredientInfo> ingredients
) {
    public ProductCandidate {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    }

    public static ProductCandidate of(Product product, List<IngredientInfo> ingredients) {
        return new ProductCandidate(
                product.getId(),
                product.getBrandName(),
                product.getProductName(),
                product.getCategory(),
                product.getImageUrl(),
                product.getPurchaseUrl(),
                product.getPrice(),
                ingredients
        );
    }
}
