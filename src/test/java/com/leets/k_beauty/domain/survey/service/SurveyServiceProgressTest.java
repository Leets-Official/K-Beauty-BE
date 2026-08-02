package com.leets.k_beauty.domain.survey.service;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.dto.SurveyOptionResponse;
import com.leets.k_beauty.domain.survey.dto.SurveyProgressResponse;
import com.leets.k_beauty.domain.survey.dto.SurveyQuestionResponse;
import com.leets.k_beauty.domain.survey.enums.NextAction;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SelectionType;
import com.leets.k_beauty.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재진입 시 "지금 무엇을 해야 하는지" 안내와 질문 메타데이터 조회 검증.
 *
 * <p>앱을 껐다 켜거나 새로고침하면 클라이언트는 진행 상태만 보고 화면을 결정해야 하므로,
 * 진행 단계별로 currentQuestionCode와 nextAction이 정확해야 한다.
 */
@DisplayName("설문 진행 상태")
class SurveyServiceProgressTest extends IntegrationTestSupport {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Nested
    @DisplayName("이어서 진행할 때")
    class Resume {

        @Test
        @DisplayName("아무것도 답하지 않았으면 첫 질문을 알려준다")
        void pointsToFirstQuestion() {
            Fixture fixture = newSurvey();

            SurveyProgressResponse progress = progressOf(fixture);

            assertThat(progress.nextAction()).isEqualTo(NextAction.ANSWER_QUESTION);
            assertThat(progress.currentQuestionCode()).isEqualTo(QuestionCode.CONCERN);
            assertThat(progress.currentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("고민만 답했으면 피부 타입을 알려준다")
        void pointsToSkinType() {
            Fixture fixture = newSurvey();
            answer(fixture, QuestionCode.CONCERN, "MOISTURE");

            SurveyProgressResponse progress = progressOf(fixture);

            assertThat(progress.currentQuestionCode()).isEqualTo(QuestionCode.SKIN_TYPE);
            assertThat(progress.currentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("피부 타입까지 답했으면 진단 경로를 고르라고 안내한다")
        void pointsToDiagnosisModeSelection() {
            Fixture fixture = answeredBasicQuestions();

            SurveyProgressResponse progress = progressOf(fixture);

            assertThat(progress.nextAction()).isEqualTo(NextAction.SELECT_DIAGNOSIS_MODE);
            assertThat(progress.currentQuestionCode()).isNull();
            assertThat(progress.diagnosisMode()).isNull();
        }

        @Test
        @DisplayName("상세 진단을 고른 뒤에는 민감 여부를 알려준다")
        void pointsToSensitivity() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");

            SurveyProgressResponse progress = progressOf(fixture);

            assertThat(progress.nextAction()).isEqualTo(NextAction.ANSWER_QUESTION);
            assertThat(progress.currentQuestionCode()).isEqualTo(QuestionCode.SENSITIVITY);
        }

        @Test
        @DisplayName("민감 '예'로 답하면 주의 요소가 남은 질문으로 나온다")
        void pointsToCaution() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            assertThat(progressOf(fixture).currentQuestionCode()).isEqualTo(QuestionCode.CAUTION);
        }

        @Test
        @DisplayName("민감 '아니요'면 주의 요소를 건너뛰고 탐색 습관을 알려준다")
        void skipsCautionWhenNotSensitive() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");

            assertThat(progressOf(fixture).currentQuestionCode())
                    .isEqualTo(QuestionCode.EXPLORATION_HABIT);
        }

        @Test
        @DisplayName("빠른 진단은 두 문항만 채워도 완료할 수 있다고 안내한다")
        void quickPathIsReadyToComplete() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "QUICK");

            SurveyProgressResponse progress = progressOf(fixture);

            assertThat(progress.nextAction()).isEqualTo(NextAction.READY_TO_COMPLETE);
            assertThat(progress.currentQuestionCode()).isNull();
        }

        @Test
        @DisplayName("상세 진단도 필수 응답을 다 채우면 완료할 수 있다고 안내한다")
        void detailedPathIsReadyToComplete() {
            Fixture fixture = answeredAllQuestions();

            assertThat(progressOf(fixture).nextAction()).isEqualTo(NextAction.READY_TO_COMPLETE);
        }
    }

    @Nested
    @DisplayName("질문을 조회하면")
    class QuestionLookup {

        @Test
        @DisplayName("선택지를 표시 순서대로 준다")
        void returnsOptionsInDisplayOrder() {
            SurveyQuestionResponse question = surveyService.getQuestion(QuestionCode.CONCERN);

            assertThat(question.questionCode()).isEqualTo(QuestionCode.CONCERN);
            assertThat(question.selectionType()).isEqualTo(SelectionType.SINGLE);
            assertThat(question.options())
                    .extracting(SurveyOptionResponse::optionCode)
                    .containsExactly("MOISTURE", "TONE", "SENSITIVE", "AGING", "TROUBLE");
        }

        @Test
        @DisplayName("피부 타입은 가이드 문구를 함께 준다")
        void skinTypeCarriesGuideText() {
            SurveyQuestionResponse question = surveyService.getQuestion(QuestionCode.SKIN_TYPE);

            assertThat(optionOf(question, "DRY").guideText()).isNotBlank();
            assertThat(optionOf(question, "UNKNOWN").guideText()).isNull();
        }

        @Test
        @DisplayName("주의 요소는 최대 선택 개수와 배타 선택지 표시를 함께 준다")
        void cautionCarriesSelectionRules() {
            SurveyQuestionResponse question = surveyService.getQuestion(QuestionCode.CAUTION);

            assertThat(question.selectionType()).isEqualTo(SelectionType.MULTIPLE);
            assertThat(question.maxSelections()).isEqualTo(4);
            assertThat(optionOf(question, "UNKNOWN").isExclusive()).isTrue();
            assertThat(optionOf(question, "FRAGRANCE").isExclusive()).isFalse();
        }
    }

    // ---- fixtures & helpers ----

    private record Fixture(Long surveyId, String sessionToken) {
    }

    private Fixture newSurvey() {
        String sessionToken = UUID.randomUUID().toString();
        sessionRepository.save(Session.create(sessionToken));
        return new Fixture(surveyService.create(sessionToken).id(), sessionToken);
    }

    private Fixture answeredBasicQuestions() {
        Fixture fixture = newSurvey();
        answer(fixture, QuestionCode.CONCERN, "MOISTURE");
        answer(fixture, QuestionCode.SKIN_TYPE, "DRY");
        return fixture;
    }

    private Fixture answeredAllQuestions() {
        Fixture fixture = answeredBasicQuestions();
        setMode(fixture, "DETAILED");
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
        answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL");
        answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");
        return fixture;
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

    private SurveyProgressResponse progressOf(Fixture fixture) {
        return surveyService.getProgress(fixture.surveyId(), fixture.sessionToken());
    }

    private SurveyOptionResponse optionOf(SurveyQuestionResponse question, String optionCode) {
        return question.options().stream()
                .filter(option -> option.optionCode().equals(optionCode))
                .findFirst()
                .orElseThrow();
    }
}