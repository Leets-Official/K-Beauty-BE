package com.leets.k_beauty.domain.product.dto;

import com.leets.k_beauty.domain.product.entity.Ingredient;

public record IngredientInfo(
        Long ingredientId,
        String name,
        String cautionDescription
) {
    public static IngredientInfo from(Ingredient ingredient) {
        return new IngredientInfo(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getCautionDescription()
        );
    }
}
