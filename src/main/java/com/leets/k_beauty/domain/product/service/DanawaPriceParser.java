package com.leets.k_beauty.domain.product.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DanawaPriceParser {

    private static final Pattern[] PRIMARY_PRICE_PATTERNS = {
            Pattern.compile("id=\"min_price_[^\"]*\"\\s+value=\"([0-9,]+)\""),
            Pattern.compile("data-productprice=\"([0-9,]+)\""),
            Pattern.compile("property=\"og:description\"\\s+content=\"최저가\\s*([0-9,]+)원\"")
    };
    private static final Pattern[] FALLBACK_PRICE_PATTERNS = {
            Pattern.compile("\"lowPrice\"\\s*:\\s*\"?([0-9,]+)\"?"),
            Pattern.compile("\"minPrice\"\\s*:\\s*\"?([0-9,]+)\"?"),
            Pattern.compile("lowestPrice[^0-9]{0,40}([0-9,]+)"),
            Pattern.compile("\"price\"\\s*:\\s*\"?([0-9,]+)\"?")
    };

    public Optional<Integer> parse(String html) {
        if (html == null || html.isBlank()) {
            return Optional.empty();
        }

        Optional<Integer> primaryPrice = findLowestPrice(html, PRIMARY_PRICE_PATTERNS);
        if (primaryPrice.isPresent()) {
            return primaryPrice;
        }

        return findLowestPrice(html, FALLBACK_PRICE_PATTERNS);
    }

    private Optional<Integer> findLowestPrice(String html, Pattern[] patterns) {
        return Arrays.stream(patterns)
                .flatMap(pattern -> findPrices(html, pattern).stream())
                .filter(price -> price > 0)
                .min(Comparator.naturalOrder());
    }

    private List<Integer> findPrices(String html, Pattern pattern) {
        List<Integer> prices = new ArrayList<>();
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String rawPrice = matcher.group(1).replace(",", "");
            try {
                prices.add(Integer.parseInt(rawPrice));
            } catch (NumberFormatException ignored) {
                // Keep scanning the next matching price pattern.
            }
        }
        return prices;
    }
}
