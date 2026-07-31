package com.leets.k_beauty.domain.product.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leets.k_beauty.domain.product.dto.IngredientSeedData;
import com.leets.k_beauty.domain.product.dto.ProductEnrichmentSeedData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String PRODUCT_ENRICHMENTS_PATH = "data/product_enrichments.json";

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
        Map<ProductSeedKey, ProductEnrichmentSeedData> enrichmentByProduct = readProductEnrichments();

        seedData.forEach(data -> {
            ProductEnrichmentSeedData enrichment = enrichmentByProduct.get(ProductSeedKey.from(data));
            String imageUrl = resolveImageUrl(enrichment);
            String purchaseUrl = resolvePurchaseUrl(enrichment);
            Integer price = resolvePrice(enrichment);
            boolean isActive = resolveIsActive(data, enrichment);

            Product product = productRepository.findByBrandNameAndProductName(data.brandName(), data.productName())
                    .orElseGet(() -> productRepository.save(Product.builder()
                            .brandName(data.brandName())
                            .productName(data.productName())
                            .category(data.category())
                            .imageUrl(imageUrl)
                            .purchaseUrl(purchaseUrl)
                            .price(price)
                            .isActive(isActive)
                            .build()));

            product.updateSeedData(data.category(), imageUrl, purchaseUrl, price, isActive);
            seedProductIngredients(product, data.ingredients());
        });
    }

    private Map<ProductSeedKey, ProductEnrichmentSeedData> readProductEnrichments() {
        List<ProductEnrichmentSeedData> seedData = readSeedData(
                PRODUCT_ENRICHMENTS_PATH,
                new TypeReference<>() {
                }
        );
        Map<ProductSeedKey, ProductEnrichmentSeedData> enrichmentByProduct = new HashMap<>();

        seedData.forEach(data -> {
            ProductSeedKey key = ProductSeedKey.from(data);
            if (enrichmentByProduct.put(key, data) != null) {
                throw new IllegalStateException("Duplicated product enrichment data: " + key);
            }
        });

        return enrichmentByProduct;
    }

    private String resolveImageUrl(ProductEnrichmentSeedData enrichment) {
        return enrichment != null && StringUtils.hasText(enrichment.imageUrl())
                ? enrichment.imageUrl()
                : null;
    }

    private String resolvePurchaseUrl(ProductEnrichmentSeedData enrichment) {
        return enrichment != null && StringUtils.hasText(enrichment.purchaseUrl())
                ? enrichment.purchaseUrl()
                : null;
    }

    private Integer resolvePrice(ProductEnrichmentSeedData enrichment) {
        return enrichment != null ? enrichment.price() : null;
    }

    private boolean resolveIsActive(ProductSeedData product, ProductEnrichmentSeedData enrichment) {
        boolean seedActive = product.isActive() == null || product.isActive();
        return seedActive && hasCompleteEnrichment(enrichment);
    }

    private boolean hasCompleteEnrichment(ProductEnrichmentSeedData enrichment) {
        return enrichment != null
                && StringUtils.hasText(enrichment.imageUrl())
                && StringUtils.hasText(enrichment.purchaseUrl())
                && enrichment.price() != null;
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

    private record ProductSeedKey(String brandName, String productName) {

        private static ProductSeedKey from(ProductSeedData data) {
            return new ProductSeedKey(data.brandName(), data.productName());
        }

        private static ProductSeedKey from(ProductEnrichmentSeedData data) {
            return new ProductSeedKey(data.brandName(), data.productName());
        }
    }
}
