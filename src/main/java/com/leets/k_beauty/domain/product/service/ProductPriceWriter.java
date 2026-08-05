package com.leets.k_beauty.domain.product.service;

import com.leets.k_beauty.domain.product.entity.Product;
import com.leets.k_beauty.domain.product.repository.ProductRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPriceWriter {

    private final ProductRepository productRepository;

    @Transactional
    public boolean updatePrice(Long productId, Integer price) {
        Optional<Product> product = productRepository.findById(productId);
        product.ifPresent(savedProduct -> savedProduct.updatePrice(price));
        return product.isPresent();
    }
}
