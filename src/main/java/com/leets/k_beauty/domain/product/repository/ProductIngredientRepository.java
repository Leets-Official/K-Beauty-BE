package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {
}
