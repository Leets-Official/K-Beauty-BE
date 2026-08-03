package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;

public record RestartResponse(
        Long previousSurveyResponseId,
        Long surveyResponseId,
        Long sessionId,
        SurveyStatus status,
        QuestionCode currentQuestionCode,
        Integer currentStep
) {
}
