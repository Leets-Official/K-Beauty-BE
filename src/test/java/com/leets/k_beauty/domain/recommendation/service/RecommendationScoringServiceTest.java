package com.leets.k_beauty.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.leets.k_beauty.domain.common.enums.SensitivityStatus;
import com.leets.k_beauty.domain.common.enums.SkinConcern;
import com.leets.k_beauty.domain.common.enums.SkinType;
import com.leets.k_beauty.domain.product.dto.IngredientInfo;
import com.leets.k_beauty.domain.product.dto.ProductCandidate;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import com.leets.k_beauty.domain.product.service.ProductQueryService;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationInput;
import com.leets.k_beauty.domain.recommendation.dto.ScoredCandidate;
import com.leets.k_beauty.domain.recommendation.enums.ExplorationHabit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationScoringServiceTest {

    @Test
    @DisplayName("매칭 성분이 없으면 피부 타입 기반 fallback 추천 이유를 반환한다")
    void fallbackReasonWhenNoIngredientMatchesConcern() {
        ProductQueryService productQueryService = mock(ProductQueryService.class);
        RecommendationScoringService scoringService = new RecommendationScoringService(productQueryService);
        ProductCandidate product = product(
                1L,
                ProductCategory.SKIN,
                12000,
                List.of(new IngredientInfo(1L, "어성초추출물", null))
        );
        when(productQueryService.findCandidatesByCategory(ProductCategory.SKIN)).thenReturn(List.of(product));

        RecommendationInput input = new RecommendationInput(
                SkinConcern.MOISTURE,
                SkinType.DRY,
                false,
                SensitivityStatus.LOW,
                ExplorationHabit.FREQUENTLY,
                List.of()
        );

        List<ScoredCandidate> candidates = scoringService.getCandidatesForStep(1, input);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).matchScore()).isZero();
        assertThat(candidates.get(0).reasonShort())
                .isEqualTo("선택한 고민과 직접 맞는 핵심 성분은 적지만, 피부 타입과 제품군의 사용감을 함께 고려해 추천했어요.");
    }

    @Test
    @DisplayName("탐색 습관이 FREQUENTLY이면 전문적인 추천 이유를 반환한다")
    void expertReasonWhenExplorationHabitIsFrequently() {
        ProductQueryService productQueryService = mock(ProductQueryService.class);
        RecommendationScoringService scoringService = new RecommendationScoringService(productQueryService);
        ProductCandidate product = product(
                1L,
                ProductCategory.SKIN,
                12000,
                List.of(
                        new IngredientInfo(1L, "글리세린", null),
                        new IngredientInfo(2L, "판테놀", null)
                )
        );
        when(productQueryService.findCandidatesByCategory(ProductCategory.SKIN)).thenReturn(List.of(product));

        RecommendationInput input = new RecommendationInput(
                SkinConcern.MOISTURE,
                SkinType.DRY,
                false,
                SensitivityStatus.LOW,
                ExplorationHabit.FREQUENTLY,
                List.of()
        );

        List<ScoredCandidate> candidates = scoringService.getCandidatesForStep(1, input);

        assertThat(candidates.get(0).matchScore()).isEqualTo(2);
        assertThat(candidates.get(0).reasonShort())
                .isEqualTo("수분 부족 고민에 맞춰 글리세린, 판테놀 성분 조합을 기준으로 추천했어요. 수분 공급과 보습 장벽 유지 관점에서 잘 맞아요.");
    }

    @Test
    @DisplayName("탐색 습관이 RARELY이면 성분명을 노출하지 않는 쉬운 추천 이유를 반환한다")
    void simpleReasonWhenExplorationHabitIsRarely() {
        ProductQueryService productQueryService = mock(ProductQueryService.class);
        RecommendationScoringService scoringService = new RecommendationScoringService(productQueryService);
        ProductCandidate product = product(
                1L,
                ProductCategory.SKIN,
                12000,
                List.of(new IngredientInfo(1L, "글리세린", null))
        );
        when(productQueryService.findCandidatesByCategory(ProductCategory.SKIN)).thenReturn(List.of(product));

        RecommendationInput input = new RecommendationInput(
                SkinConcern.MOISTURE,
                SkinType.DRY,
                false,
                SensitivityStatus.LOW,
                ExplorationHabit.RARELY,
                List.of()
        );

        List<ScoredCandidate> candidates = scoringService.getCandidatesForStep(1, input);

        assertThat(candidates.get(0).matchScore()).isEqualTo(1);
        assertThat(candidates.get(0).reasonShort())
                .isEqualTo("건조한 피부에 수분감을 채워주기 좋아 추천했어요.");
    }

    private ProductCandidate product(
            Long productId,
            ProductCategory category,
            Integer price,
            List<IngredientInfo> ingredients
    ) {
        return new ProductCandidate(
                productId,
                "브랜드",
                "상품",
                category,
                "https://example.com/image.png",
                "https://example.com/product",
                price,
                ingredients
        );
    }
}
