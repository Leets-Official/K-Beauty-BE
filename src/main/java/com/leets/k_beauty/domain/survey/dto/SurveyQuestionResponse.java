package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.entity.SurveyOption;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SelectionType;

import java.util.List;

public record SurveyQuestionResponse(
        QuestionCode questionCode,
        String questionText,
        SelectionType selectionType,
        Integer maxSelections,
        Integer surveyStep,
        Integer displayOrder,
        boolean isRequired,
        List<SurveyOptionResponse> options
) {
    public static SurveyQuestionResponse from(Survey survey, List<SurveyOption> options) {
        return new SurveyQuestionResponse(
                survey.getQuestionCode(),
                survey.getQuestionText(),
                survey.getSelectionType(),
                survey.getMaxSelections(),
                survey.getSurveyStep(),
                survey.getDisplayOrder(),
                survey.isRequired(),
                options.stream().map(SurveyOptionResponse::from).toList()
        );
    }
}
