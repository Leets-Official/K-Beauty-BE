package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.enums.DiagnosisMode;

import java.time.LocalDateTime;

public record DiagnosisModeResponse(
        Long surveyId,
        DiagnosisMode diagnosisMode,
        LocalDateTime updatedAt
) {
    public static DiagnosisModeResponse from(UserCondition userCondition) {
        return new DiagnosisModeResponse(
                userCondition.getId(),
                userCondition.getDiagnosisMode(),
                userCondition.getUpdatedAt()
        );
    }
}
