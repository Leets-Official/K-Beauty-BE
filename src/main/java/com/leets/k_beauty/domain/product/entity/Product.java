package com.leets.k_beauty.domain.product.entity;

import com.leets.k_beauty.domain.product.enums.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 100, unique = true)
    private String externalId;

    @Column(name = "brand_name", nullable = false, length = 50)
    private String brandName;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductCategory category;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "purchase_url", length = 500)
    private String purchaseUrl;

    @Column
    private Integer price;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Product(
            String externalId,
            String brandName,
            String productName,
            ProductCategory category,
            String imageUrl,
            String purchaseUrl,
            Integer price,
            Boolean isActive
    ) {
        this.externalId = externalId;
        this.brandName = brandName;
        this.productName = productName;
        this.category = category;
        this.imageUrl = imageUrl;
        this.purchaseUrl = purchaseUrl;
        this.price = price;
        this.isActive = isActive != null ? isActive : true;
    }

    public void updateSeedData(
            ProductCategory category,
            String imageUrl,
            String purchaseUrl,
            Integer price,
            Boolean isActive
    ) {
        this.category = category;
        this.imageUrl = imageUrl;
        this.purchaseUrl = purchaseUrl;
        this.price = price;
        this.isActive = isActive != null ? isActive : true;
    }

    public void updateSeedDataPreservingPrice(
            ProductCategory category,
            String imageUrl,
            String purchaseUrl,
            Boolean isActive
    ) {
        this.category = category;
        this.imageUrl = imageUrl;
        this.purchaseUrl = purchaseUrl;
        this.isActive = isActive != null ? isActive : true;
    }

    public void updatePrice(Integer price) {
        if (price != null) {
            this.price = price;
        }
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
