package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.enums.DiagnosisMode;
import com.leets.k_beauty.domain.survey.enums.NextAction;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SensitivityStatus;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;

import java.util.List;

public record SurveyProgressResponse(
        Long surveyResponseId,
        SurveyStatus status,
        DiagnosisMode diagnosisMode,
        SensitivityStatus sensitivityStatus,
        boolean typeNeutralMode,
        List<AnsweredQuestion> answers,
        QuestionCode currentQuestionCode,
        Integer currentStep,
        NextAction nextAction
) {
    public record AnsweredQuestion(
            QuestionCode questionCode,
            List<String> optionCodes
    ) {
    }
}
