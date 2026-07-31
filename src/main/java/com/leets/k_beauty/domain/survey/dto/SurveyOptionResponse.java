package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.SurveyOption;

public record SurveyOptionResponse(
        String optionCode,
        String optionText,
        String guideText,
        Integer displayOrder,
        boolean isExclusive
) {
    public static SurveyOptionResponse from(SurveyOption option) {
        return new SurveyOptionResponse(
                option.getOptionCode(),
                option.getOptionText(),
                option.getGuideText(),
                option.getDisplayOrder(),
                option.isExclusive()
        );
    }
}
