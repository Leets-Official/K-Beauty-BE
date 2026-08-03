package com.leets.k_beauty.domain.recommendation.service;

import com.leets.k_beauty.domain.product.dto.ProductCandidate;
import com.leets.k_beauty.domain.product.enums.ProductCategory;
import com.leets.k_beauty.domain.product.service.ProductQueryService;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.common.enums.CautionCategory;
import com.leets.k_beauty.domain.common.enums.SensitivityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 추천 점수 계산 및 후보 선정 서비스.
 *
 * 점수 기준:
 *   필터: sensitivityStatus가 MEDIUM/HIGH이고 사용자 주의 카테고리와 성분이 겹치는 제품 제외
 *         (IngredientInfo.cautionDescription 키워드 기반 매핑)
 *
 * [TODO - 상품팀 협의]
 * Product에 아래 메타데이터가 추가되면 점수 로직을 구현합니다:
 *   - skinConcerns : 피부 고민 매칭 +3점
 *   - skinTypes    : 피부 타입 매칭 +2점 (typeNeutral 모드 시 typeNeutral=true +2점)
 *   - rating       : 동점자 정렬 기준
 */
@Service
@RequiredArgsConstructor
public class RecommendationScoringService {

    private static final int CANDIDATE_COUNT = 3;

    private final ProductQueryService productQueryService;

    /**
     * 각 단계(1~3)에 맞는 후보 ProductCandidate 3개를 반환한다.
     * rank 1 = 메인 추천, rank 2~3 = 다른 후보
     */
    public List<ProductCandidate> getCandidatesForStep(int step, Session session) {
        List<ProductCategory> categories = categoriesForStep(step);
        List<ProductCandidate> pool = fetchPool(categories);
        return filter(pool, session).stream()
                .limit(CANDIDATE_COUNT)
                .toList();
    }

    // ---- private helpers ----

    private List<ProductCategory> categoriesForStep(int step) {
        return switch (step) {
            case 1 -> List.of(ProductCategory.TONER, ProductCategory.SKIN);
            case 2 -> List.of(ProductCategory.ESSENCE, ProductCategory.SERUM, ProductCategory.AMPOULE);
            case 3 -> step3Categories();
            default -> throw new IllegalArgumentException("유효하지 않은 단계입니다: " + step);
        };
    }

    /**
     * 4~9월 = 가벼운 텍스처(GEL_CREAM) 우선
     * 10~3월 = 진한 텍스처(NUTRIENT_DEEP_CREAM) 우선
     */
    private List<ProductCategory> step3Categories() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 4 && month <= 9) {
            return List.of(
                    ProductCategory.GEL_CREAM,
                    ProductCategory.MOISTURE_CREAM,
                    ProductCategory.NUTRIENT_DEEP_CREAM
            );
        }
        return List.of(
                ProductCategory.NUTRIENT_DEEP_CREAM,
                ProductCategory.MOISTURE_CREAM,
                ProductCategory.GEL_CREAM
        );
    }

    /** 카테고리 순서대로 ProductQueryService를 호출해 후보 풀을 구성한다. */
    private List<ProductCandidate> fetchPool(List<ProductCategory> categories) {
        List<ProductCandidate> pool = new ArrayList<>();
        for (ProductCategory category : categories) {
            pool.addAll(productQueryService.findCandidatesByCategory(category));
        }
        return pool;
    }

    /**
     * 민감도 필터 적용.
     * MEDIUM/HIGH인 경우 사용자 주의 카테고리와 성분이 겹치는 제품을 제외한다.
     *
     * TODO: Product 메타데이터 추가 후 피부 고민·타입 기반 점수 로직 구현
     */
    private List<ProductCandidate> filter(List<ProductCandidate> pool, Session session) {
        boolean shouldFilter = session.getSensitivityStatus() == SensitivityStatus.MEDIUM
                || session.getSensitivityStatus() == SensitivityStatus.HIGH;

        if (!shouldFilter || session.getCautionCategories().isEmpty()) {
            return pool;
        }

        return pool.stream()
                .filter(candidate -> !hasCautionConflict(candidate, session.getCautionCategories()))
                .toList();
    }

    /**
     * 제품의 성분 주의사항 텍스트와 사용자 주의 카테고리를 키워드 기반으로 매핑한다.
     * cautionDescription이 null인 성분은 주의 없음으로 처리한다.
     */
    private boolean hasCautionConflict(ProductCandidate candidate, List<CautionCategory> userCautions) {
        return candidate.ingredients().stream()
                .filter(ing -> ing.cautionDescription() != null)
                .anyMatch(ing -> userCautions.stream()
                        .anyMatch(caution -> matchesCaution(ing.cautionDescription(), caution)));
    }

    private boolean matchesCaution(String cautionDescription, CautionCategory caution) {
        String lower = cautionDescription.toLowerCase();
        return switch (caution) {
            case FRAGRANCE -> lower.contains("향료") || lower.contains("fragrance") || lower.contains("parfum");
            case ALCOHOL -> lower.contains("알코올") || lower.contains("에탄올") || lower.contains("alcohol");
            case OIL -> lower.contains("오일") || lower.contains("oil");
            case EXFOLIANT -> lower.contains("aha") || lower.contains("bha") || lower.contains("acid") || lower.contains("산");
            case UNKNOWN -> false;
        };
    }
}
