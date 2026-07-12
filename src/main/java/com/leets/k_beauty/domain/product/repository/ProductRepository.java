package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByExternalProductId(String externalProductId);

    Optional<Product> findByBrandNameAndName(String brandName, String name);

    boolean existsByBrandNameAndName(String brandName, String name);
}
