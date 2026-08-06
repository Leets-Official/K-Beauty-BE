package com.leets.k_beauty.domain.recommendation.service;

import com.leets.k_beauty.domain.common.enums.CautionCategory;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationScoringService {

    private static final int CANDIDATE_COUNT = 3;

    private final ProductQueryService productQueryService;

    // 피부 고민별 추천 성분
    private static final Map<SkinConcern, Set<String>> CONCERN_INGREDIENT_MAP = Map.of(
            SkinConcern.MOISTURE,
            Set.of("글리세린", "히알루론산", "저분자 히알루론산", "베타인", "판테놀", "세라마이드", "스쿠알란", "시어버터"),

            SkinConcern.SENSITIVE,
            Set.of("병풀추출물", "시카", "어성초추출물", "티트리", "알란토인", "판테놀", "마데카소사이드", "쑥추출물"),

            SkinConcern.TONE,
            Set.of("나이아신아마이드", "비타민 c", "비타민 c 유도체", "트라넥사믹애씨드", "알부틴"),

            SkinConcern.TROUBLE,
            Set.of("aha", "bha", "pha", "위치하젤", "레티놀", "징크 pca", "발효성분", "갈락토미세스", "비피다발효용해물", "나이아신아마이드"),

            SkinConcern.AGING,
            Set.of("레티놀", "펩타이드", "아데노신", "나이아신아마이드", "발효성분", "비피다발효용해물", "세라마이드")
    );

    private static final Map<SkinConcern, String> CONCERN_DISPLAY_NAME = Map.of(
            SkinConcern.MOISTURE, "수분 부족",
            SkinConcern.SENSITIVE, "예민함",
            SkinConcern.TONE, "피부톤 불균형",
            SkinConcern.TROUBLE, "트러블/모공",
            SkinConcern.AGING, "탄력/주름"
    );

    private static final Map<SkinConcern, String> CONCERN_EFFECT_DESCRIPTION = Map.of(
            SkinConcern.MOISTURE, "수분감을 채워주는 데 도움을 줄 수 있어요.",
            SkinConcern.SENSITIVE, "피부를 편안하게 진정시키는 데 도움을 줄 수 있어요.",
            SkinConcern.TONE, "칙칙한 피부톤을 맑아 보이게 가꾸는 데 도움을 줄 수 있어요.",
            SkinConcern.TROUBLE, "피부결과 모공 고민을 케어하는 데 도움을 줄 수 있어요.",
            SkinConcern.AGING, "탄력과 주름 고민을 케어하는 데 도움을 줄 수 있어요."
    );

    private static final Map<SkinConcern, String> CONCERN_EXPERT_DESCRIPTION = Map.of(
            SkinConcern.MOISTURE, "수분 공급과 보습 장벽 유지 관점에서 잘 맞아요.",
            SkinConcern.SENSITIVE, "진정 케어와 자극 부담 완화 관점에서 잘 맞아요.",
            SkinConcern.TONE, "칙칙함 케어와 톤 균일도 관리 관점에서 잘 맞아요.",
            SkinConcern.TROUBLE, "피부결 케어와 유분 밸런스 관리 관점에서 잘 맞아요.",
            SkinConcern.AGING, "탄력 케어와 보습 장벽 관리 관점에서 잘 맞아요."
    );

    private static final Map<SkinConcern, String> CONCERN_SIMPLE_REASON = Map.of(
            SkinConcern.MOISTURE, "건조한 피부에 수분감을 채워주기 좋아 추천했어요.",
            SkinConcern.SENSITIVE, "예민한 피부도 편안하게 쓰기 좋아 추천했어요.",
            SkinConcern.TONE, "칙칙한 피부톤이 고민일 때 쓰기 좋아 추천했어요.",
            SkinConcern.TROUBLE, "번들거림이나 모공 고민이 있는 피부에 가볍게 쓰기 좋아 추천했어요.",
            SkinConcern.AGING, "탄력이 떨어져 보이는 피부를 관리하기 좋아 추천했어요."
    );

    private enum Season { SUMMER, SPRING_FALL, WINTER }

    /**
     * 각 단계(1~3)에 맞는 후보 ScoredCandidate 3개를 반환한다.
     * rank 1 = 메인 추천, rank 2~3 = 다른 후보
     */
    public List<ScoredCandidate> getCandidatesForStep(int step, RecommendationInput input) {
        SkinType skinType = input.skinType() != null ? input.skinType() : SkinType.UNKNOWN;
        List<CautionCategory> cautionCategories = effectiveCautionCategories(input);
        boolean hasOilDiscomfort = cautionCategories.contains(CautionCategory.OIL);

        List<ProductCategory> categories = categoriesForStep(step, skinType, hasOilDiscomfort);
        List<ProductCandidate> pool = fetchPool(categories);
        List<ProductCandidate> filtered = filterCaution(pool, input.sensitivityStatus(), cautionCategories);
        return scoreAndSort(filtered, input.skinConcern(), input.explorationHabit())
                .stream()
                .limit(CANDIDATE_COUNT)
                .toList();
    }

    // ---- 카테고리 선택 ----

    private List<ProductCategory> categoriesForStep(int step, SkinType skinType, boolean hasOilDiscomfort) {
        return switch (step) {
            case 1 -> step1Categories(skinType);
            case 2 -> step2Categories(skinType);
            case 3 -> List.of(step3Category(skinType, hasOilDiscomfort));
            default -> throw new IllegalArgumentException("유효하지 않은 단계입니다: " + step);
        };
    }

    private List<ProductCategory> step1Categories(SkinType skinType) {
        return switch (skinType) {
            case DRY, DEHYDRATED_OILY -> List.of(ProductCategory.SKIN);
            case OILY, COMBINATION, UNKNOWN -> List.of(ProductCategory.TONER);
        };
    }

    private List<ProductCategory> step2Categories(SkinType skinType) {
        return switch (skinType) {
            case DRY -> List.of(ProductCategory.AMPOULE);
            case OILY -> List.of(ProductCategory.ESSENCE);
            case COMBINATION, DEHYDRATED_OILY, UNKNOWN -> List.of(ProductCategory.SERUM);
        };
    }

    /**
     * 오일 불편이 있으면 피부타입 기본 크림을 한 단계 낮추고 계절 변환을 생략한다.
     * 오일 불편이 없으면 피부타입 + 계절 조합 테이블을 적용한다.
     */
    private ProductCategory step3Category(SkinType skinType, boolean hasOilDiscomfort) {
        if (hasOilDiscomfort) {
            return downgradeCategory(baseStep3Category(skinType));
        }
        return seasonalStep3Category(skinType);
    }

    private ProductCategory baseStep3Category(SkinType skinType) {
        return switch (skinType) {
            case DRY -> ProductCategory.NUTRIENT_DEEP_CREAM;
            case OILY -> ProductCategory.GEL_CREAM;
            case COMBINATION, DEHYDRATED_OILY, UNKNOWN -> ProductCategory.MOISTURE_CREAM;
        };
    }

    /** NUTRIENT_DEEP → MOISTURE → GEL (이미 GEL이면 유지) */
    private ProductCategory downgradeCategory(ProductCategory base) {
        return switch (base) {
            case NUTRIENT_DEEP_CREAM -> ProductCategory.MOISTURE_CREAM;
            case MOISTURE_CREAM -> ProductCategory.GEL_CREAM;
            default -> ProductCategory.GEL_CREAM;
        };
    }

    /**
     * 피부타입 + 계절 조합 테이블
     * 여름 6~9월 / 봄가을 3~5·10월 / 겨울 11~2월
     */
    private ProductCategory seasonalStep3Category(SkinType skinType) {
        Season season = toSeason(LocalDate.now().getMonthValue());
        return switch (skinType) {
            case DRY -> switch (season) {
                case SUMMER -> ProductCategory.MOISTURE_CREAM;
                case SPRING_FALL, WINTER -> ProductCategory.NUTRIENT_DEEP_CREAM;
            };
            case OILY -> switch (season) {
                case SUMMER, SPRING_FALL -> ProductCategory.GEL_CREAM;
                case WINTER -> ProductCategory.MOISTURE_CREAM;
            };
            case COMBINATION, DEHYDRATED_OILY, UNKNOWN -> switch (season) {
                case SUMMER -> ProductCategory.GEL_CREAM;
                case SPRING_FALL -> ProductCategory.MOISTURE_CREAM;
                case WINTER -> ProductCategory.NUTRIENT_DEEP_CREAM;
            };
        };
    }

    private Season toSeason(int month) {
        if (month >= 6 && month <= 9) return Season.SUMMER;
        if (month >= 3 && month <= 5 || month == 10) return Season.SPRING_FALL;
        return Season.WINTER;
    }

    // ---- 풀 조회 ----

    private List<ProductCandidate> fetchPool(List<ProductCategory> categories) {
        List<ProductCandidate> pool = new ArrayList<>();
        for (ProductCategory category : categories) {
            pool.addAll(productQueryService.findCandidatesByCategory(category));
        }
        return pool;
    }

    // ---- 민감도 필터 ----

    private List<CautionCategory> effectiveCautionCategories(RecommendationInput input) {
        if (input.sensitivityStatus() != SensitivityStatus.MEDIUM
                && input.sensitivityStatus() != SensitivityStatus.HIGH) {
            return List.of();
        }
        return input.cautionCategories();
    }

    private List<ProductCandidate> filterCaution(
            List<ProductCandidate> pool,
            SensitivityStatus sensitivityStatus,
            List<CautionCategory> cautionCategories
    ) {
        boolean shouldFilter = sensitivityStatus == SensitivityStatus.MEDIUM
                || sensitivityStatus == SensitivityStatus.HIGH;

        if (!shouldFilter || cautionCategories.isEmpty()) {
            return pool;
        }

        return pool.stream()
                .filter(candidate -> !hasCautionConflict(candidate, cautionCategories))
                .toList();
    }

    private boolean hasCautionConflict(ProductCandidate candidate, List<CautionCategory> userCautions) {
        return candidate.ingredients().stream()
                .anyMatch(ing -> userCautions.stream()
                        .anyMatch(caution -> matchesCaution(ing, caution)));
    }

    private boolean matchesCaution(IngredientInfo ing, CautionCategory caution) {
        String name = normalizeIngredientText(ing.name());
        String desc = normalizeIngredientText(ing.cautionDescription());
        return switch (caution) {
            case FRAGRANCE ->
                    name.contains("향료") || name.contains("fragrance") || name.contains("parfum")
                    || desc.contains("향료") || desc.contains("fragrance");
            case ALCOHOL ->
                    // 자극감에 예민: 알코올 계열 + AHA/BHA/PHA + 레티놀 + 비타민C + 티트리 제외
                    name.contains("알코올") || name.contains("에탄올") || name.contains("alcohol")
                    || name.contains("aha") || name.contains("bha") || name.contains("pha")
                    || name.contains("레티놀") || name.contains("retinol")
                    || name.contains("비타민c") || name.contains("ascorbic")
                    || name.contains("티트리") || name.contains("teatree")
                    || desc.contains("알코올") || desc.contains("에탄올");
            case OIL ->
                    name.contains("오일") || name.contains("oil")
                    || desc.contains("오일") || desc.contains("oil");
            case EXFOLIANT ->
                    name.contains("aha") || name.contains("bha") || name.contains("pha")
                    || desc.contains("aha") || desc.contains("bha")
                    || desc.contains("acid") || desc.contains("산");
            case UNKNOWN -> false;
        };
    }

    // ---- 점수 계산 및 정렬 ----

    private List<ScoredCandidate> scoreAndSort(
            List<ProductCandidate> pool,
            SkinConcern skinConcern,
            ExplorationHabit explorationHabit
    ) {
        Set<String> concernIngredients = skinConcern != null
                ? CONCERN_INGREDIENT_MAP.getOrDefault(skinConcern, Set.of())
                : Set.of();
        Set<String> normalizedConcernIngredients = concernIngredients.stream()
                .map(this::normalizeIngredientText)
                .filter(ingredient -> !ingredient.isBlank())
                .collect(Collectors.toSet());

        return pool.stream()
                .map(product -> {
                    List<String> matched = matchedIngredientNames(product, normalizedConcernIngredients);
                    List<String> reasonIngredientNames = matched.stream()
                            .limit(3)
                            .toList();
                    return new ScoredCandidate(
                            product,
                            matched.size(),
                            buildReason(reasonIngredientNames, skinConcern, explorationHabit)
                    );
                })
                .sorted(Comparator.comparingInt(ScoredCandidate::matchScore).reversed()
                        .thenComparing(candidate -> candidate.product().price(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(candidate -> candidate.product().productId()))
                .collect(Collectors.toList());
    }

    /** 제품 성분 중 피부 고민 성분과 일치하는 원본 성분명 목록 */
    private List<String> matchedIngredientNames(ProductCandidate product, Set<String> concernIngredients) {
        if (concernIngredients.isEmpty()) return List.of();
        return product.ingredients().stream()
                .map(IngredientInfo::name)
                .filter(Objects::nonNull)
                .filter(ing -> concernIngredients.stream()
                        .anyMatch(ci -> normalizeIngredientText(ing).contains(ci)))
                .distinct()
                .toList();
    }

    private String normalizeIngredientText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private String buildReason(
            List<String> matchedNames,
            SkinConcern concern,
            ExplorationHabit explorationHabit
    ) {
        if (matchedNames.isEmpty() || concern == null) {
            return buildFallbackReason(explorationHabit);
        }

        if (explorationHabit == ExplorationHabit.RARELY) {
            return CONCERN_SIMPLE_REASON.getOrDefault(
                    concern,
                    "선택한 피부 고민에 맞춰 쓰기 좋아 추천했어요."
            );
        }

        String concernName = CONCERN_DISPLAY_NAME.getOrDefault(concern, "선택한 피부");
        String ingredientPhrase = toIngredientPhrase(matchedNames);
        if (explorationHabit == ExplorationHabit.FREQUENTLY) {
            String expertDescription = CONCERN_EXPERT_DESCRIPTION.getOrDefault(
                    concern,
                    "선택한 고민을 케어하는 성분 구성 관점에서 잘 맞아요."
            );
            return concernName + " 고민에 맞춰 " + ingredientPhrase
                    + "을 기준으로 추천했어요. " + expertDescription;
        }

        String effectDescription = CONCERN_EFFECT_DESCRIPTION.getOrDefault(
                concern,
                "선택한 고민을 케어하는 데 도움을 줄 수 있어요."
        );
        return concernName + " 고민에 맞게 " + joinIngredientNames(matchedNames)
                + " 성분이 " + effectDescription;
    }

    private String buildFallbackReason(ExplorationHabit explorationHabit) {
        return switch (explorationHabit) {
            case FREQUENTLY -> "선택한 고민과 직접 맞는 핵심 성분은 적지만, 피부 타입과 제품군의 사용감을 함께 고려해 추천했어요.";
            case OCCASIONALLY -> "피부 타입과 제품군을 기준으로, 매일 부담 없이 사용할 수 있는 제품으로 추천했어요.";
            case RARELY -> "피부 타입에 맞춰 사용감이 무난한 제품으로 추천했어요.";
        };
    }

    private String toIngredientPhrase(List<String> ingredientNames) {
        if (ingredientNames.size() == 1) {
            return ingredientNames.get(0) + " 성분";
        }
        return joinIngredientNames(ingredientNames) + " 성분 조합";
    }

    private String joinIngredientNames(List<String> ingredientNames) {
        return String.join(", ", ingredientNames);
    }

}
