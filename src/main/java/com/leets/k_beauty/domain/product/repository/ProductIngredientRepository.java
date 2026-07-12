package com.leets.k_beauty.domain.product.repository;

import com.leets.k_beauty.domain.product.entity.Ingredient;
import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.entity.ProductIngredient;
import com.leets.k_beauty.domain.product.entity.ProductIngredientId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, ProductIngredientId> {

    List<ProductIngredient> findByProduct(Product product);

    boolean existsByProductAndIngredient(Product product, Ingredient ingredient);
}
