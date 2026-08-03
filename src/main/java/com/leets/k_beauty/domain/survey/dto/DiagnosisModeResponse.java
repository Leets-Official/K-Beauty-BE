package com.leets.k_beauty.domain.survey.dto;

import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.enums.DiagnosisMode;
import com.leets.k_beauty.domain.survey.enums.NextAction;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SensitivityStatus;

import java.time.LocalDateTime;

public record DiagnosisModeResponse(
        Long surveyId,
        DiagnosisMode diagnosisMode,
        SensitivityStatus sensitivityStatus,
        NextAction nextAction,
        QuestionCode nextQuestionCode,
        LocalDateTime updatedAt
) {
    // 진단 경로를 고르면 다음에 어디로 갈지가 정해지므로, 그 정보를 응답에 함께 담음
    // 상세 진단: 다음 질문 코드, 빠른 진단: 비어 있음.
    public static DiagnosisModeResponse of(UserCondition userCondition,
                                           NextAction nextAction,
                                           QuestionCode nextQuestionCode) {
        return new DiagnosisModeResponse(
                userCondition.getId(),
                userCondition.getDiagnosisMode(),
                userCondition.getSensitivityStatus(),
                nextAction,
                nextQuestionCode,
                userCondition.getUpdatedAt()
        );
    }
}