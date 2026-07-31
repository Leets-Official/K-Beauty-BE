package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.ProductIngredient;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {

    @Query("""
            SELECT productIngredient
            FROM ProductIngredient productIngredient
            JOIN FETCH productIngredient.product product
            JOIN FETCH productIngredient.ingredient ingredient
            WHERE product.id = :productId
            ORDER BY productIngredient.displayOrder ASC
            """)
    List<ProductIngredient> findByProductId(@Param("productId") Long productId);

    @Query("""
            SELECT productIngredient
            FROM ProductIngredient productIngredient
            JOIN FETCH productIngredient.product product
            JOIN FETCH productIngredient.ingredient ingredient
            WHERE product.id IN :productIds
            ORDER BY product.id ASC, productIngredient.displayOrder ASC
            """)
    List<ProductIngredient> findByProductIds(@Param("productIds") Collection<Long> productIds);
}
