package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
