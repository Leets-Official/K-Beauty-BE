package com.leets.k_beauty.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ingredients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "caution_description", columnDefinition = "TEXT")
    private String cautionDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Ingredient(String name, String description, String cautionDescription) {
        this.name = name;
        this.description = description;
        this.cautionDescription = cautionDescription;
    }

    public void updateCautionDescription(String cautionDescription) {
        this.cautionDescription = cautionDescription;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
