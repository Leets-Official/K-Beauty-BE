package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.enums.NextAction;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.RecommendationImpact;
import com.leets.k_beauty.domain.survey.enums.SensitivityStatus;

import java.util.List;

public record AnswerSaveResponse(
        Long surveyResponseId,
        QuestionCode questionCode,
        List<String> savedOptionCodes,
        List<QuestionCode> clearedQuestionCodes,
        SensitivityStatus derivedSensitivityStatus,
        NextAction nextAction,
        QuestionCode nextQuestionCode,
        RecommendationImpact recommendationImpact
) {
}
