package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBrandNameAndProductName(String brandName, String productName);

    // 특정 상품 카테고리에 속한 활성 상품 목록을 조회한다.
    @Query("""
            SELECT product
            FROM Product product
            WHERE product.category = :category
              AND product.isActive = true
            ORDER BY product.id ASC
            """)
    List<Product> findActiveByCategory(@Param("category") ProductCategory category);

    @Query("""
            SELECT product
            FROM Product product
            WHERE product.isActive = true
              AND product.purchaseUrl IS NOT NULL
              AND product.purchaseUrl <> ''
            ORDER BY product.id ASC
            """)
    List<Product> findActiveWithPurchaseUrl();
}
