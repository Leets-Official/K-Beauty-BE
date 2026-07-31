package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.entity.SurveyOption;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SelectionType;

import java.util.List;

public record PreviousQuestionResponse(
        QuestionCode questionCode,
        String questionText,
        SelectionType selectionType,
        Integer maxSelections,
        List<String> selectedOptionCodes,
        List<SurveyOptionResponse> options
) {
    public static PreviousQuestionResponse of(Survey survey, List<SurveyOption> options, List<String> selectedOptionCodes) {
        return new PreviousQuestionResponse(
                survey.getQuestionCode(),
                survey.getQuestionText(),
                survey.getSelectionType(),
                survey.getMaxSelections(),
                selectedOptionCodes,
                options.stream().map(SurveyOptionResponse::from).toList()
        );
    }
}