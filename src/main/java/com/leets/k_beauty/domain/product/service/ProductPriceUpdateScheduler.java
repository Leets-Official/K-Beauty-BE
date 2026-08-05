package com.leets.k_beauty.domain.product.service;

import com.leets.k_beauty.domain.product.dto.ProductPriceUpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "product.price-update.schedule-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ProductPriceUpdateScheduler {

    private final ProductPriceUpdateService productPriceUpdateService;

    @Scheduled(cron = "0 0 7,23 * * *", zone = "Asia/Seoul")
    public void updateDailyPrices() {
        log.info("상품 가격 정기 갱신 시작");
        ProductPriceUpdateResult result = productPriceUpdateService.updatePrices();
        log.info("상품 가격 정기 갱신 완료 - totalCount={}, updatedCount={}, skippedCount={}, failedCount={}",
                result.totalCount(), result.updatedCount(), result.skippedCount(), result.failedCount());
    }
}
