package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.enums.DiagnosisMode;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;

import java.time.LocalDateTime;

public record UserConditionResponse(
        Long id,
        Long sessionId,
        DiagnosisMode diagnosisMode,
        SurveyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public static UserConditionResponse from(UserCondition userCondition) {
        return new UserConditionResponse(
                userCondition.getId(),
                userCondition.getSessionId(),
                userCondition.getDiagnosisMode(),
                userCondition.getStatus(),
                userCondition.getCreatedAt(),
                userCondition.getUpdatedAt(),
                userCondition.getCompletedAt()
        );
    }
}
