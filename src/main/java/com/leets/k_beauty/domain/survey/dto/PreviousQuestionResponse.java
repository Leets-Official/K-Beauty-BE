package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.entity.SurveyOption;
import com.leets.k_beauty.domain.survey.enums.NextAction;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SelectionType;

import java.util.List;

public record PreviousQuestionResponse(
        NextAction nextAction,
        QuestionCode questionCode,
        String questionText,
        SelectionType selectionType,
        Integer maxSelections,
        List<String> selectedOptionCodes,
        List<SurveyOptionResponse> options
) {
    // 더 돌아갈 질문이 없을 때. 질문 정보 없이 온보딩으로 가라는 안내만 담는다
    public static PreviousQuestionResponse onboarding() {
        return new PreviousQuestionResponse(
                NextAction.GO_TO_ONBOARDING, null, null, null, null, null, null);
    }

    public static PreviousQuestionResponse of(Survey survey, List<SurveyOption> options, List<String> selectedOptionCodes) {
        return new PreviousQuestionResponse(
                NextAction.ANSWER_QUESTION,
                survey.getQuestionCode(),
                survey.getQuestionText(),
                survey.getSelectionType(),
                survey.getMaxSelections(),
                selectedOptionCodes,
                options.stream().map(SurveyOptionResponse::from).toList()
        );
    }
}