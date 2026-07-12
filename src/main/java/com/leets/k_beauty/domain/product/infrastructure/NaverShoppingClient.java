package com.leets.k_beauty.domain.product.infrastructure;

import com.leets.k_beauty.domain.product.dto.NaverShoppingSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class NaverShoppingClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public NaverShoppingClient(
            @Value("${naver.shopping.base-url}") String baseUrl,
            @Value("${naver.shopping.client-id}") String clientId,
            @Value("${naver.shopping.client-secret}") String clientSecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public NaverShoppingSearchResponse search(String query) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new IllegalStateException("Naver shopping API credentials are not configured.");
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/shop.json")
                        .queryParam("query", query)
                        .queryParam("display", 5)
                        .queryParam("sort", "sim")
                        .queryParam("exclude", "used:rental:cbshop")
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .body(NaverShoppingSearchResponse.class);
    }
}
