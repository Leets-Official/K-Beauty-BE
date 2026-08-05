package com.leets.k_beauty.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ProductPriceCrawlerTest {

    private final ProductPriceCrawler crawler = new ProductPriceCrawler(RestClient.builder(), new DanawaPriceParser());

    @Test
    @DisplayName("다나와 info 링크는 그대로 크롤링 대상 URL로 사용한다")
    void resolveDanawaInfoUrl() {
        Optional<String> url = crawler.resolveCrawlUrl("https://prod.danawa.com/info/?pcode=4926856");

        assertThat(url).contains("https://prod.danawa.com/info/?pcode=4926856");
    }

    @Test
    @DisplayName("prod_id가 있는 다나와 bridge 링크는 info 링크로 변환한다")
    void resolveDanawaBridgeUrlWithProductId() {
        Optional<String> url = crawler.resolveCrawlUrl(
                "https://prod.danawa.com/bridge/go_link_goods.php?prod_id=18716279&link_prod_c=F260375353"
        );

        assertThat(url).contains("https://prod.danawa.com/info/?pcode=18716279");
    }

    @Test
    @DisplayName("link_prod_c만 있는 다나와 bridge 링크는 오매칭 방지를 위해 제외한다")
    void skipDanawaBridgeUrlWithOnlyLinkProductCode() {
        Optional<String> url = crawler.resolveCrawlUrl(
                "https://prod.danawa.com/bridge/go_link_goods.php?link_prod_c=8380720201"
        );

        assertThat(url).isEmpty();
    }
}
