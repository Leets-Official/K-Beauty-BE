package com.leets.k_beauty.domain.product.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leets.k_beauty.domain.product.dto.IngredientSeedData;
import com.leets.k_beauty.domain.product.dto.ProductSeedData;
import com.leets.k_beauty.domain.product.dto.ProductSeedIngredient;
import com.leets.k_beauty.domain.product.entity.Ingredient;
import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.entity.ProductIngredient;
import com.leets.k_beauty.domain.product.repository.IngredientRepository;
import com.leets.k_beauty.domain.product.repository.ProductIngredientRepository;
import com.leets.k_beauty.domain.product.repository.ProductRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "product.seed.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ProductDataInitializer implements ApplicationRunner {

    private static final String INGREDIENTS_PATH = "data/ingredients.json";
    private static final String PRODUCTS_PATH = "data/products.json";

    private final ObjectMapper objectMapper;
    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedIngredients();
        seedProducts();
    }

    private void seedIngredients() {
        List<IngredientSeedData> seedData = readSeedData(
                INGREDIENTS_PATH,
                new TypeReference<>() {
                }
        );

        seedData.forEach(data -> {
            Ingredient ingredient = ingredientRepository.findByName(data.name())
                    .orElseGet(() -> ingredientRepository.save(Ingredient.builder()
                            .name(data.name())
                            .cautionDescription(data.cautionDescription())
                            .build()));

            if (StringUtils.hasText(data.cautionDescription())) {
                ingredient.updateCautionDescription(data.cautionDescription());
            }
        });
    }

    private void seedProducts() {
        List<ProductSeedData> seedData = readSeedData(
                PRODUCTS_PATH,
                new TypeReference<>() {
                }
        );

        seedData.forEach(data -> {
            Product product = productRepository.findByBrandNameAndProductName(data.brandName(), data.productName())
                    .orElseGet(() -> productRepository.save(Product.builder()
                            .brandName(data.brandName())
                            .productName(data.productName())
                            .category(data.category())
                            .isActive(true)
                            .build()));

            seedProductIngredients(product, data.ingredients());
        });
    }

    private void seedProductIngredients(Product product, List<ProductSeedIngredient> seedIngredients) {
        if (seedIngredients == null || seedIngredients.isEmpty()) {
            return;
        }

        for (int i = 0; i < seedIngredients.size(); i++) {
            ProductSeedIngredient seedIngredient = seedIngredients.get(i);
            Ingredient ingredient = findOrCreateIngredient(seedIngredient.name());
            if (productIngredientRepository.existsByProductAndIngredient(product, ingredient)) {
                continue;
            }

            productIngredientRepository.save(ProductIngredient.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .displayOrder(resolveDisplayOrder(seedIngredient, i))
                    .build());
        }
    }

    private Ingredient findOrCreateIngredient(String name) {
        return ingredientRepository.findByName(name)
                .orElseGet(() -> ingredientRepository.save(Ingredient.builder()
                        .name(name)
                        .build()));
    }

    private Integer resolveDisplayOrder(ProductSeedIngredient seedIngredient, int index) {
        return seedIngredient.displayOrder() != null ? seedIngredient.displayOrder() : index + 1;
    }

    private <T> List<T> readSeedData(String path, TypeReference<List<T>> typeReference) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read product seed data: " + path, e);
        }
    }
}
