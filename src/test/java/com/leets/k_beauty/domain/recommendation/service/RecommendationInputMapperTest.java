package com.leets.k_beauty.domain.recommendation.service;

import com.leets.k_beauty.domain.common.enums.CautionCategory;
import com.leets.k_beauty.domain.common.enums.SensitivityStatus;
import com.leets.k_beauty.domain.common.enums.SkinConcern;
import com.leets.k_beauty.domain.common.enums.SkinType;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationInput;
import com.leets.k_beauty.domain.recommendation.enums.ExplorationHabit;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.repository.UserConditionRepository;
import com.leets.k_beauty.domain.survey.repository.UserSurveyAnswerRepository;
import com.leets.k_beauty.domain.survey.service.SurveyService;
import com.leets.k_beauty.support.IntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("추천 입력 변환")
class RecommendationInputMapperTest extends IntegrationTestSupport {

    @Autowired
    private RecommendationInputMapper recommendationInputMapper;

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserConditionRepository userConditionRepository;

    @Autowired
    private UserSurveyAnswerRepository userSurveyAnswerRepository;

    @Test
    @DisplayName("설문 답변을 추천 입력값으로 변환한다")
    void mapsSurveyAnswersToRecommendationInput() {
        Fixture fixture = readyForRecommendationInput();
        answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE", "EXFOLIATION");

        RecommendationInput input = toRecommendationInput(fixture);

        assertThat(input.skinConcern()).isEqualTo(SkinConcern.MOISTURE);
        assertThat(input.skinType()).isEqualTo(SkinType.DRY);
        assertThat(input.typeNeutralMode()).isFalse();
        assertThat(input.sensitivityStatus()).isEqualTo(SensitivityStatus.HIGH);
        assertThat(input.explorationHabit()).isEqualTo(ExplorationHabit.OCCASIONALLY);
        assertThat(input.cautionCategories())
                .containsExactlyInAnyOrder(CautionCategory.OIL, CautionCategory.EXFOLIANT);
    }

    @Test
    @DisplayName("피부 타입을 모르면 타입 중립 입력값으로 변환한다")
    void mapsUnknownSkinTypeToTypeNeutralInput() {
        Fixture fixture = newSurvey();
        answer(fixture, QuestionCode.CONCERN, "TONE");
        answer(fixture, QuestionCode.SKIN_TYPE, "UNKNOWN");

        RecommendationInput input = toRecommendationInput(fixture);

        assertThat(input.skinConcern()).isEqualTo(SkinConcern.TONE);
        assertThat(input.skinType()).isEqualTo(SkinType.UNKNOWN);
        assertThat(input.typeNeutralMode()).isTrue();
        assertThat(input.sensitivityStatus()).isEqualTo(SensitivityStatus.UNASSESSED);
        assertThat(input.explorationHabit()).isEqualTo(ExplorationHabit.OCCASIONALLY);
        assertThat(input.cautionCategories()).isEmpty();
    }

    @Test
    @DisplayName("탐색 습관 답변을 추천 입력값으로 변환한다")
    void mapsExplorationHabitToRecommendationInput() {
        Fixture fixture = readyForRecommendationInput();
        answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE");
        answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");

        RecommendationInput input = toRecommendationInput(fixture);

        assertThat(input.explorationHabit()).isEqualTo(ExplorationHabit.FREQUENTLY);
    }

    @Test
    @DisplayName("QUICK 전환 시 남아있는 주의 요소 답변은 추천 입력에 반영하지 않는다")
    void quickModeIgnoresRemainingCautionAnswers() {
        Fixture fixture = readyForRecommendationInput();
        answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE", "EXFOLIATION");

        setMode(fixture, "QUICK");
        RecommendationInput input = toRecommendationInput(fixture);

        assertThat(input.sensitivityStatus()).isEqualTo(SensitivityStatus.UNASSESSED);
        assertThat(input.explorationHabit()).isEqualTo(ExplorationHabit.OCCASIONALLY);
        assertThat(input.cautionCategories()).isEmpty();
    }

    private RecommendationInput toRecommendationInput(Fixture fixture) {
        UserCondition userCondition = userConditionRepository.findById(fixture.surveyId()).orElseThrow();
        return recommendationInputMapper.from(
                userCondition,
                userSurveyAnswerRepository.findAllByCondition(userCondition)
        );
    }

    private Fixture readyForRecommendationInput() {
        Fixture fixture = newSurvey();
        answer(fixture, QuestionCode.CONCERN, "MOISTURE");
        answer(fixture, QuestionCode.SKIN_TYPE, "DRY");
        setMode(fixture, "DETAILED");
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
        return fixture;
    }

    private Fixture newSurvey() {
        String sessionToken = newSessionToken();
        return new Fixture(surveyService.create(sessionToken).id(), sessionToken);
    }

    private String newSessionToken() {
        String rawToken = UUID.randomUUID().toString();
        sessionRepository.save(Session.create(rawToken));
        return rawToken;
    }

    private void answer(Fixture fixture, QuestionCode questionCode, String... optionCodes) {
        surveyService.saveAnswer(
                fixture.surveyId(), questionCode,
                new AnswerSaveRequest(List.of(optionCodes)), fixture.sessionToken());
    }

    private void setMode(Fixture fixture, String diagnosisMode) {
        surveyService.setDiagnosisMode(
                fixture.surveyId(), new DiagnosisModeRequest(diagnosisMode), fixture.sessionToken());
    }

    private record Fixture(Long surveyId, String sessionToken) {
    }
}
