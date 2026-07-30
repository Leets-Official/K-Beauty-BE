package com.leets.k_beauty.domain.product.dto;

import com.leets.k_beauty.domain.product.entity.Ingredient;

public record IngredientResponse(
        Long ingredientId,
        String name,
        String cautionDescription
) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getCautionDescription()
        );
    }
}
