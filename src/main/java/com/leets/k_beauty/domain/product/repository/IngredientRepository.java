package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.Ingredient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    Optional<Ingredient> findByName(String name);

    boolean existsByName(String name);
}
