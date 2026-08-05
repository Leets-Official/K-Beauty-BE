package com.leets.k_beauty.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.leets.k_beauty.domain.product.dto.ProductPriceUpdateResult;
import com.leets.k_beauty.domain.product.dto.ProductPriceUpdateTarget;
import com.leets.k_beauty.domain.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductPriceUpdateServiceTest {

    @Test
    @DisplayName("가격 크롤링은 트랜잭션 없이 진행하고 저장은 writer에 위임한다")
    void delegatesPricePersistenceToWriter() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductPriceCrawler productPriceCrawler = mock(ProductPriceCrawler.class);
        ProductPriceWriter productPriceWriter = mock(ProductPriceWriter.class);
        ProductPriceUpdateService service = new ProductPriceUpdateService(
                productRepository,
                productPriceCrawler,
                productPriceWriter
        );
        ProductPriceUpdateTarget updated = target(1L, "https://example.com/updated");
        ProductPriceUpdateTarget skipped = target(2L, "https://example.com/skipped");
        ProductPriceUpdateTarget failed = target(3L, "https://example.com/failed");
        ProductPriceUpdateTarget missing = target(4L, "https://example.com/missing");

        when(productRepository.findPriceUpdateTargets()).thenReturn(List.of(updated, skipped, failed, missing));
        when(productPriceCrawler.fetchPrice(updated.purchaseUrl())).thenReturn(Optional.of(10000));
        when(productPriceWriter.updatePrice(updated.productId(), 10000)).thenReturn(true);
        when(productPriceCrawler.fetchPrice(skipped.purchaseUrl())).thenReturn(Optional.empty());
        when(productPriceCrawler.fetchPrice(failed.purchaseUrl())).thenThrow(new RuntimeException("timeout"));
        when(productPriceCrawler.fetchPrice(missing.purchaseUrl())).thenReturn(Optional.of(12000));
        when(productPriceWriter.updatePrice(missing.productId(), 12000)).thenReturn(false);

        ProductPriceUpdateResult result = service.updatePrices();

        assertThat(result.totalCount()).isEqualTo(4);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(2);
        verify(productPriceWriter).updatePrice(updated.productId(), 10000);
        verify(productPriceWriter).updatePrice(missing.productId(), 12000);
        verifyNoMoreInteractions(productPriceWriter);
    }

    private ProductPriceUpdateTarget target(Long productId, String purchaseUrl) {
        return new ProductPriceUpdateTarget(productId, "브랜드", "상품", purchaseUrl);
    }
}
