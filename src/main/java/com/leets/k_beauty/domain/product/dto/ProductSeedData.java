package com.leets.k_beauty.domain.product.dto;

import com.leets.k_beauty.domain.product.enums.ProductCategory;
import java.util.List;

public record ProductSeedData(
        String brandName,
        String productName,
        ProductCategory category,
        String naverSearchQuery,
        Boolean isActive,
        List<ProductSeedIngredient> ingredients
) {
}
