package com.leets.k_beauty.domain.product.service;

import com.leets.k_beauty.domain.product.dto.ProductPriceUpdateResult;
import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPriceUpdateService {

    private final ProductRepository productRepository;
    private final ProductPriceCrawler productPriceCrawler;

    @Transactional
    public ProductPriceUpdateResult updatePrices() {
        List<Product> products = productRepository.findActiveWithPurchaseUrl();

        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Product product : products) {
            try {
                Optional<Integer> price = productPriceCrawler.fetchPrice(product.getPurchaseUrl());
                if (price.isPresent()) {
                    product.updatePrice(price.get());
                    updatedCount++;
                    continue;
                }

                skippedCount++;
            } catch (RuntimeException e) {
                failedCount++;
                log.warn("상품 가격 갱신 실패 - productId={}, productName={}",
                        product.getId(), formatProductName(product), e);
            }
        }

        return new ProductPriceUpdateResult(products.size(), updatedCount, skippedCount, failedCount);
    }

    private String formatProductName(Product product) {
        return product.getBrandName() + " " + product.getProductName();
    }
}
