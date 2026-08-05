package com.leets.k_beauty.domain.product.service;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductPriceCrawler {

    private static final String DANAWA_HOST = "prod.danawa.com";
    private static final String DANAWA_INFO_PATH = "/info/";
    private static final String DANAWA_BRIDGE_PATH = "/bridge/";
    private static final Pattern PRODUCT_ID_PARAM_PATTERN = Pattern.compile("[?&](?:pcode|prod_id)=([0-9]+)");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final DanawaPriceParser danawaPriceParser;

    public ProductPriceCrawler(RestClient.Builder restClientBuilder, DanawaPriceParser danawaPriceParser) {
        this.restClient = restClientBuilder.clone()
                .requestFactory(requestFactory())
                .build();
        this.danawaPriceParser = danawaPriceParser;
    }

    public Optional<Integer> fetchPrice(String purchaseUrl) {
        Optional<String> crawlUrl = resolveCrawlUrl(purchaseUrl);
        if (crawlUrl.isEmpty()) {
            return Optional.empty();
        }

        String html = restClient.get()
                .uri(crawlUrl.get())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("User-Agent", "Mozilla/5.0")
                .retrieve()
                .body(String.class);

        return danawaPriceParser.parse(html);
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    Optional<String> resolveCrawlUrl(String purchaseUrl) {
        if (purchaseUrl == null || purchaseUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = URI.create(purchaseUrl);
            if (!DANAWA_HOST.equalsIgnoreCase(uri.getHost()) || uri.getPath() == null) {
                return Optional.empty();
            }
            if (uri.getPath().startsWith(DANAWA_INFO_PATH)) {
                return Optional.of(purchaseUrl);
            }
            if (uri.getPath().startsWith(DANAWA_BRIDGE_PATH)) {
                return resolveDanawaBridgeInfoUrl(uri);
            }
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<String> resolveDanawaBridgeInfoUrl(URI uri) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = PRODUCT_ID_PARAM_PATTERN.matcher("?" + query);
        if (matcher.find()) {
            return Optional.of("https://" + DANAWA_HOST + DANAWA_INFO_PATH + "?pcode=" + matcher.group(1));
        }

        return Optional.empty();
    }
}
