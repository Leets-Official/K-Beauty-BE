package com.leets.k_beauty.domain.recommendation.service;

import com.leets.k_beauty.domain.common.enums.CautionCategory;
import com.leets.k_beauty.domain.common.enums.SensitivityStatus;
import com.leets.k_beauty.domain.common.enums.SkinConcern;
import com.leets.k_beauty.domain.common.enums.SkinType;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationInput;
import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.entity.UserSurveyAnswer;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.global.exception.BusinessException;
import com.leets.k_beauty.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RecommendationInputMapper {

    public RecommendationInput from(UserCondition userCondition, List<UserSurveyAnswer> answers) {
        Map<QuestionCode, List<String>> optionCodesByQuestion = groupOptionCodes(answers);

        return new RecommendationInput(
                toSkinConcern(singleOptionCode(optionCodesByQuestion, QuestionCode.CONCERN)),
                toSkinType(singleOptionCode(optionCodesByQuestion, QuestionCode.SKIN_TYPE)),
                userCondition.isTypeNeutralMode(),
                toSensitivityStatus(userCondition.getSensitivityStatus()),
                toCautionCategories(optionCodesByQuestion.getOrDefault(QuestionCode.CAUTION, List.of()))
        );
    }

    private Map<QuestionCode, List<String>> groupOptionCodes(List<UserSurveyAnswer> answers) {
        List<UserSurveyAnswer> safeAnswers = answers == null ? List.of() : answers;
        return safeAnswers.stream()
                .collect(Collectors.groupingBy(
                        answer -> answer.getOption().getSurvey().getQuestionCode(),
                        Collectors.mapping(answer -> answer.getOption().getOptionCode(), Collectors.toList())
                ));
    }

    private String singleOptionCode(Map<QuestionCode, List<String>> optionCodesByQuestion, QuestionCode questionCode) {
        List<String> optionCodes = optionCodesByQuestion.getOrDefault(questionCode, List.of());
        if (optionCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.SURVEY_ANSWER_MISSING);
        }
        if (optionCodes.size() > 1) {
            throw new BusinessException(ErrorCode.SURVEY_OPTION_INVALID);
        }
        return optionCodes.get(0);
    }

    private SkinConcern toSkinConcern(String optionCode) {
        return switch (optionCode) {
            case "MOISTURE" -> SkinConcern.MOISTURE;
            case "TONE" -> SkinConcern.TONE;
            case "SENSITIVE" -> SkinConcern.SENSITIVE;
            case "AGING" -> SkinConcern.AGING;
            case "TROUBLE" -> SkinConcern.TROUBLE;
            default -> throw new BusinessException(ErrorCode.SURVEY_OPTION_INVALID);
        };
    }

    private SkinType toSkinType(String optionCode) {
        return switch (optionCode) {
            case "DRY" -> SkinType.DRY;
            case "OILY" -> SkinType.OILY;
            case "COMBINATION" -> SkinType.COMBINATION;
            case "DEHYDRATED_OILY" -> SkinType.DEHYDRATED_OILY;
            case "UNKNOWN" -> SkinType.UNKNOWN;
            default -> throw new BusinessException(ErrorCode.SURVEY_OPTION_INVALID);
        };
    }

    private SensitivityStatus toSensitivityStatus(
            com.leets.k_beauty.domain.survey.enums.SensitivityStatus sensitivityStatus
    ) {
        if (sensitivityStatus == null) {
            return SensitivityStatus.UNASSESSED;
        }
        return switch (sensitivityStatus) {
            case UNASSESSED -> SensitivityStatus.UNASSESSED;
            case LOW -> SensitivityStatus.LOW;
            case MEDIUM -> SensitivityStatus.MEDIUM;
            case HIGH -> SensitivityStatus.HIGH;
        };
    }

    private List<CautionCategory> toCautionCategories(List<String> optionCodes) {
        return optionCodes.stream()
                .map(this::toCautionCategory)
                .toList();
    }

    private CautionCategory toCautionCategory(String optionCode) {
        return switch (optionCode) {
            case "FRAGRANCE" -> CautionCategory.FRAGRANCE;
            case "ALCOHOL" -> CautionCategory.ALCOHOL;
            case "OILY_TEXTURE" -> CautionCategory.OIL;
            case "EXFOLIATION" -> CautionCategory.EXFOLIANT;
            case "UNKNOWN" -> CautionCategory.UNKNOWN;
            default -> throw new BusinessException(ErrorCode.SURVEY_OPTION_INVALID);
        };
    }
}
