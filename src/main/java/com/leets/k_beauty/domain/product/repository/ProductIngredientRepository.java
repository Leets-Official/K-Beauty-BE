package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.Ingredient;
import com.leets.k_beauty.domain.product.entity.Product;
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
            JOIN FETCH productIngredient.ingredient ingredient
            WHERE productIngredient.product = :product
            """)
    List<ProductIngredient> findByProduct(@Param("product") Product product);

    // 여러 상품의 대표 성분을 상품별 표시 순서대로 한 번에 조회한다.
    @Query("""
            SELECT productIngredient
            FROM ProductIngredient productIngredient
            JOIN FETCH productIngredient.product product
            JOIN FETCH productIngredient.ingredient ingredient
            WHERE product.id IN :productIds
            ORDER BY product.id ASC,
              CASE WHEN productIngredient.displayOrder IS NULL THEN 1 ELSE 0 END ASC,
              productIngredient.displayOrder ASC
            """)
    List<ProductIngredient> findByProductIds(@Param("productIds") Collection<Long> productIds);
}
