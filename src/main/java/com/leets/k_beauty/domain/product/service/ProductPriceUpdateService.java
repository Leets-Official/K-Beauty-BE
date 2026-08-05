package com.leets.k_beauty.domain.product.service;

import com.leets.k_beauty.domain.product.dto.ProductPriceUpdateResult;
import com.leets.k_beauty.domain.product.dto.ProductPriceUpdateTarget;
import com.leets.k_beauty.domain.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPriceUpdateService {

    private final ProductRepository productRepository;
    private final ProductPriceCrawler productPriceCrawler;
    private final ProductPriceWriter productPriceWriter;

    public ProductPriceUpdateResult updatePrices() {
        List<ProductPriceUpdateTarget> targets = productRepository.findPriceUpdateTargets();

        int updatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (ProductPriceUpdateTarget target : targets) {
            try {
                Optional<Integer> price = productPriceCrawler.fetchPrice(target.purchaseUrl());
                if (price.isPresent()) {
                    if (productPriceWriter.updatePrice(target.productId(), price.get())) {
                        updatedCount++;
                    } else {
                        failedCount++;
                        log.warn("상품 가격 갱신 실패 - 상품을 찾을 수 없음. productId={}, productName={}",
                                target.productId(), formatProductName(target));
                    }
                    continue;
                }

                skippedCount++;
            } catch (RuntimeException e) {
                failedCount++;
                log.warn("상품 가격 갱신 실패 - productId={}, productName={}",
                        target.productId(), formatProductName(target), e);
            }
        }

        return new ProductPriceUpdateResult(targets.size(), updatedCount, skippedCount, failedCount);
    }

    private String formatProductName(ProductPriceUpdateTarget target) {
        return target.brandName() + " " + target.productName();
    }
}
