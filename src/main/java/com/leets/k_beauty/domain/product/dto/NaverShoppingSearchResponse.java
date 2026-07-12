package com.leets.k_beauty.domain.product.dto;

import java.util.List;

public record NaverShoppingSearchResponse(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NaverShoppingItem> items
) {
}
