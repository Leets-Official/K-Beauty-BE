package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT product
            FROM Product product
            WHERE product.isActive = true
            """)
    List<Product> findActive();

    @Query("""
            SELECT product
            FROM Product product
            WHERE product.category = :category
              AND product.isActive = true
            """)
    List<Product> findActiveByCategory(@Param("category") ProductCategory category);

    @Query("""
            SELECT product
            FROM Product product
            WHERE product.category IN :categories
              AND product.isActive = true
            ORDER BY product.category ASC, product.id ASC
            """)
    List<Product> findActiveByCategories(@Param("categories") Collection<ProductCategory> categories);

    @Query("""
            SELECT product
            FROM Product product
            WHERE product.id = :productId
              AND product.isActive = true
            """)
    Optional<Product> findActiveById(@Param("productId") Long productId);

    @Query("""
            SELECT product
            FROM Product product
            WHERE product.id IN :productIds
              AND product.isActive = true
            ORDER BY product.id ASC
            """)
    List<Product> findActiveByIds(@Param("productIds") Collection<Long> productIds);
}
