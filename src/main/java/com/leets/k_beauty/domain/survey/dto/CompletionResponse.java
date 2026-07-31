package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.enums.DiagnosisMode;
import com.leets.k_beauty.domain.survey.enums.SensitivityStatus;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;

import java.time.LocalDateTime;

public record CompletionResponse(
        Long surveyResponseId,
        SurveyStatus status,
        DiagnosisMode diagnosisMode,
        SensitivityStatus sensitivityStatus,
        boolean typeNeutralMode,
        LocalDateTime completedAt
) {
    public static CompletionResponse from(UserCondition userCondition) {
        return new CompletionResponse(
                userCondition.getId(),
                userCondition.getStatus(),
                userCondition.getDiagnosisMode(),
                userCondition.getSensitivityStatus(),
                userCondition.isTypeNeutralMode(),
                userCondition.getCompletedAt()
        );
    }
}
