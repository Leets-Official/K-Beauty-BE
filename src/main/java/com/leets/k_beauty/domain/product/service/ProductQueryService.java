package com.leets.k_beauty.domain.product.service;

import com.leets.k_beauty.domain.product.dto.IngredientInfo;
import com.leets.k_beauty.domain.product.dto.ProductCandidate;
import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.entity.ProductIngredient;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import com.leets.k_beauty.domain.product.repository.ProductIngredientRepository;
import com.leets.k_beauty.domain.product.repository.ProductRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;

    public List<ProductCandidate> findCandidatesByCategory(ProductCategory category) {
        if (category == null) {
            return List.of();
        }
        return findCandidatesByCategories(List.of(category));
    }

    public List<ProductCandidate> findCandidatesByCategories(Collection<ProductCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<Product> products = productRepository.findActiveByCategories(categories);
        return toCandidates(products);
    }

    public List<ProductCandidate> findCandidatesByIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        List<Product> products = productRepository.findActiveByIds(productIds);
        return toCandidates(products);
    }

    private List<ProductCandidate> toCandidates(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();
        Map<Long, List<IngredientInfo>> ingredientsByProductId = findIngredientsByProductId(productIds);

        return products.stream()
                .map(product -> ProductCandidate.of(
                        product,
                        ingredientsByProductId.getOrDefault(product.getId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<IngredientInfo>> findIngredientsByProductId(List<Long> productIds) {
        return productIngredientRepository.findByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(
                        productIngredient -> productIngredient.getProduct().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toIngredientInfo, Collectors.toList())
                ));
    }

    private IngredientInfo toIngredientInfo(ProductIngredient productIngredient) {
        return IngredientInfo.from(productIngredient.getIngredient());
    }
}
