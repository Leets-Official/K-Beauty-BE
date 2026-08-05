package com.leets.k_beauty.domain.recommendation.dto;

import com.leets.k_beauty.domain.common.enums.CautionCategory;
import com.leets.k_beauty.domain.common.enums.SensitivityStatus;
import com.leets.k_beauty.domain.common.enums.SkinConcern;
import com.leets.k_beauty.domain.common.enums.SkinType;
import java.util.List;

public record RecommendationInput(
        SkinConcern skinConcern,
        SkinType skinType,
        boolean typeNeutralMode,
        SensitivityStatus sensitivityStatus,
        List<CautionCategory> cautionCategories
) {

    public RecommendationInput {
        if (sensitivityStatus == null) {
            sensitivityStatus = SensitivityStatus.UNASSESSED;
        }
        cautionCategories = cautionCategories == null ? List.of() : List.copyOf(cautionCategories);
    }
}
