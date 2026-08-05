package com.leets.k_beauty.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DanawaPriceParserTest {

    private final DanawaPriceParser parser = new DanawaPriceParser();

    @Test
    @DisplayName("다나와 min_price hidden input에서 가격을 추출한다")
    void parsePriceFromDanawaMinPriceInput() {
        String html = """
                <input type="hidden" id="min_price_4926856" value="9,860" />
                """;

        assertThat(parser.parse(html)).contains(9860);
    }

    @Test
    @DisplayName("다나와 data-productprice 속성에서 가격을 추출한다")
    void parsePriceFromDanawaProductPriceAttribute() {
        String html = """
                <li class="item" data-productcode="4926856" data-productprice="9860">
                """;

        assertThat(parser.parse(html)).contains(9860);
    }

    @Test
    @DisplayName("다나와 og description에서 가격을 추출한다")
    void parsePriceFromDanawaOgDescription() {
        String html = """
                <meta property="og:description" content="최저가 9,860원"/>
                """;

        assertThat(parser.parse(html)).contains(9860);
    }

    @Test
    @DisplayName("JSON-LD price 값에서 가격을 추출한다")
    void parsePriceFromJsonLd() {
        String html = """
                <script type="application/ld+json">
                {"@type":"Product","name":"상품","offers":{"price":"12,340"}}
                </script>
                """;

        assertThat(parser.parse(html)).contains(12340);
    }

    @Test
    @DisplayName("다나와 최저가 패턴이 있으면 일반 price 값보다 우선한다")
    void preferDanawaPrimaryPricePattern() {
        String html = """
                <input type="hidden" id="min_price_4926856" value="9,860" />
                <script type="application/ld+json">
                {"@type":"Product","name":"상품","offers":{"price":"2,500"}}
                </script>
                """;

        assertThat(parser.parse(html)).contains(9860);
    }

    @Test
    @DisplayName("가격을 찾지 못하면 빈 값을 반환한다")
    void returnEmptyWhenPriceMissing() {
        assertThat(parser.parse("<html></html>")).isEmpty();
    }
}
