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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        Set<String> ingredientSeedNames = seedIngredients();
        seedProducts(ingredientSeedNames);
    }

    private Set<String> seedIngredients() {
        List<IngredientSeedData> seedData = readSeedData(
                INGREDIENTS_PATH,
                new TypeReference<>() {
                }
        );
        Set<String> ingredientSeedNames = new HashSet<>();

        seedData.forEach(data -> {
            String ingredientName = normalizeIngredientName(data.name(), INGREDIENTS_PATH);
            if (!ingredientSeedNames.add(ingredientName)) {
                throw new IllegalStateException("Duplicated ingredient seed data: " + ingredientName);
            }

            Ingredient ingredient = ingredientRepository.findByName(ingredientName)
                    .orElseGet(() -> ingredientRepository.save(Ingredient.builder()
                            .name(ingredientName)
                            .cautionDescription(data.cautionDescription())
                            .build()));

            if (StringUtils.hasText(data.cautionDescription())) {
                ingredient.updateCautionDescription(data.cautionDescription());
            }
        });

        return ingredientSeedNames;
    }

    private void seedProducts(Set<String> ingredientSeedNames) {
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
            boolean hasValidIngredients = hasValidProductIngredients(data, ingredientSeedNames);
            boolean isActive = resolveIsActive(data, enrichment) && hasValidIngredients;

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
            if (hasValidIngredients) {
                seedProductIngredients(product, data.ingredients());
            } else {
                clearProductIngredients(product);
            }
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

    private void seedProductIngredients(
            Product product,
            List<ProductSeedIngredient> seedIngredients
    ) {
        List<ProductIngredient> existingProductIngredients = productIngredientRepository.findByProduct(product);
        Map<String, ProductIngredient> existingByIngredientName = new HashMap<>();
        existingProductIngredients.forEach(productIngredient -> {
            String savedIngredientName = productIngredient.getIngredient().getName();
            String ingredientName = normalizeIngredientName(savedIngredientName, formatProductName(product));
            if (!savedIngredientName.equals(ingredientName)) {
                return;
            }

            existingByIngredientName.put(
                    ingredientName,
                    productIngredient
            );
        });

        Set<String> seedIngredientNames = new HashSet<>();
        for (int i = 0; i < seedIngredients.size(); i++) {
            ProductSeedIngredient seedIngredient = seedIngredients.get(i);
            String ingredientName = normalizeIngredientName(seedIngredient.name(), formatProductName(product));
            if (!seedIngredientNames.add(ingredientName)) {
                throw new IllegalStateException(
                        "Duplicated product ingredient data: "
                                + formatProductName(product)
                                + " - " + ingredientName
                );
            }

            Ingredient ingredient = findIngredientOrThrow(product, ingredientName);
            Integer displayOrder = resolveDisplayOrder(seedIngredient, i);
            ProductIngredient productIngredient = existingByIngredientName.get(ingredientName);
            if (productIngredient != null) {
                productIngredient.updateDisplayOrder(displayOrder);
                continue;
            }

            productIngredientRepository.save(ProductIngredient.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .displayOrder(displayOrder)
                    .build());
        }

        List<ProductIngredient> removedProductIngredients = existingProductIngredients.stream()
                .filter(productIngredient -> shouldRemoveProductIngredient(product, productIngredient, seedIngredientNames))
                .toList();
        productIngredientRepository.deleteAll(removedProductIngredients);
    }

    private boolean hasValidProductIngredients(
            ProductSeedData product,
            Set<String> ingredientSeedNames
    ) {
        List<ProductSeedIngredient> seedIngredients = product.ingredients();
        if (seedIngredients == null || seedIngredients.isEmpty()) {
            return false;
        }

        String productName = formatProductName(product);
        Set<String> seedIngredientNames = new HashSet<>();
        for (ProductSeedIngredient seedIngredient : seedIngredients) {
            String ingredientName = normalizeIngredientName(seedIngredient.name(), productName);
            if (!seedIngredientNames.add(ingredientName)) {
                throw new IllegalStateException(
                        "Duplicated product ingredient data: "
                                + productName
                                + " - " + ingredientName
                );
            }
            if (!ingredientSeedNames.contains(ingredientName)) {
                throw new IllegalStateException(
                        "Unknown product ingredient data: "
                                + productName
                                + " - " + ingredientName
                );
            }
        }
        return true;
    }

    private void clearProductIngredients(Product product) {
        List<ProductIngredient> productIngredients = productIngredientRepository.findByProduct(product);
        productIngredientRepository.deleteAll(productIngredients);
    }

    private boolean shouldRemoveProductIngredient(
            Product product,
            ProductIngredient productIngredient,
            Set<String> seedIngredientNames
    ) {
        String savedIngredientName = productIngredient.getIngredient().getName();
        String ingredientName = normalizeIngredientName(savedIngredientName, formatProductName(product));
        return !savedIngredientName.equals(ingredientName) || !seedIngredientNames.contains(ingredientName);
    }

    private Ingredient findIngredientOrThrow(Product product, String name) {
        return ingredientRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown product ingredient data: "
                                + formatProductName(product)
                                + " - " + name
                ));
    }

    private String normalizeIngredientName(String name, String source) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("Blank ingredient name: " + source);
        }
        return name.trim();
    }

    private String formatProductName(ProductSeedData product) {
        return product.brandName() + " " + product.productName();
    }

    private String formatProductName(Product product) {
        return product.getBrandName() + " " + product.getProductName();
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
