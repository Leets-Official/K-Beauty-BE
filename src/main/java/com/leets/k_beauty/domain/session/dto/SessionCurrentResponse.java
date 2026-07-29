package com.leets.k_beauty.domain.session.dto;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.enums.*;

import java.util.List;

public record SessionCurrentResponse(
        String sessionToken,
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
            List<CautionCategory> cautionCategories,
            SearchHabit searchHabit
    ) {}

    public static SessionCurrentResponse from(Session session) {
        SurveyAnswers surveyAnswers = new SurveyAnswers(
                session.getSkinConcern(),
                session.getSkinType(),
                session.isTypeNeutralMode(),
                session.getSensitivityStatus(),
                session.getCautionCategories(),
                session.getSearchHabit()
        );
        return new SessionCurrentResponse(
                session.getSessionToken(),
                session.getDiagnosisType(),
                session.getStatus(),
                surveyAnswers,
                session.getRecommendationId()
        );
    }
}
