package com.leets.k_beauty.domain.product.controller;

import com.leets.k_beauty.domain.product.dto.NaverShoppingSearchResponse;
import com.leets.k_beauty.domain.product.infrastructure.NaverShoppingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final NaverShoppingClient naverShoppingClient;

    @GetMapping("/naver")
    public NaverShoppingSearchResponse search(@RequestParam String query) {
        return naverShoppingClient.search(query);
    }
}