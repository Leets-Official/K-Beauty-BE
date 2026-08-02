package com.leets.k_beauty.domain.session.dto;

import com.leets.k_beauty.domain.common.enums.*;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.enums.SessionStatus;

import java.util.List;

public record SessionCurrentResponse(
        DiagnosisType diagnosisType,
        SessionStatus status,
        SurveyAnswers surveyAnswers,
        Long recommendationId
) {
    public record SurveyAnswers(
            SkinConcern skinConcern,
            SkinType skinType,
            boolean typeNeutralMode,
            SensitivityStatus sensitivityStatus,
            List<CautionCategory> cautionCategories
    ) {}

    public static SessionCurrentResponse from(Session session) {
        SurveyAnswers surveyAnswers = new SurveyAnswers(
                session.getSkinConcern(),
                session.getSkinType(),
                session.isTypeNeutralMode(),
                session.getSensitivityStatus(),
                session.getCautionCategories()
        );
        return new SessionCurrentResponse(
                session.getDiagnosisType(),
                session.getStatus(),
                surveyAnswers,
                session.getRecommendationId()
        );
    }
}
