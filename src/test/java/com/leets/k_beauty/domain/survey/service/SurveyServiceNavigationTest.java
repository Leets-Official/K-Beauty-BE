package com.leets.k_beauty.domain.survey.service;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveResponse;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeResponse;
import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.entity.UserSurveyAnswer;
import com.leets.k_beauty.domain.survey.enums.NextAction;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.RecommendationImpact;
import com.leets.k_beauty.domain.survey.enums.SelectionType;
import com.leets.k_beauty.domain.survey.enums.SensitivityStatus;
import com.leets.k_beauty.domain.survey.repository.UserConditionRepository;
import com.leets.k_beauty.domain.survey.repository.UserSurveyAnswerRepository;
import com.leets.k_beauty.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 다음 질문 안내(정방향 이동)와 같은 값 재저장 처리 검증.
 *
 * <p>"다음" 버튼은 답변 변경 여부와 무관하게 항상 saveAnswer를 호출한다는 규약을 전제로 한다.
 * 그래서 값이 그대로면 저장을 건너뛰고, 이동에 필요한 다음 질문만 알려줘야 한다.
 */
@DisplayName("설문 진행 안내")
class SurveyServiceNavigationTest extends IntegrationTestSupport {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserConditionRepository userConditionRepository;

    @Autowired
    private UserSurveyAnswerRepository userSurveyAnswerRepository;

    @Nested
    @DisplayName("진단 경로를 고르면")
    class DiagnosisModePointer {

        @Test
        @DisplayName("상세 진단은 다음 질문 코드를 알려준다")
        void detailedPointsToNextQuestion() {
            Fixture fixture = answeredBasicQuestions();

            DiagnosisModeResponse response = setMode(fixture, "DETAILED");

            assertThat(response.nextAction()).isEqualTo(NextAction.ANSWER_QUESTION);
            assertThat(response.nextQuestionCode()).isEqualTo(QuestionCode.SENSITIVITY);
        }

        @Test
        @DisplayName("빠른 진단은 다음 질문 없이 완료 단계로 안내한다")
        void quickPointsToCompletion() {
            Fixture fixture = answeredBasicQuestions();

            DiagnosisModeResponse response = setMode(fixture, "QUICK");

            assertThat(response.nextAction()).isEqualTo(NextAction.READY_TO_COMPLETE);
            assertThat(response.nextQuestionCode()).isNull();
        }

        @Test
        @DisplayName("재산출된 민감도 상태가 응답에 함께 실린다")
        void responseCarriesSensitivityStatus() {
            Fixture fixture = detailedWithCautionTwo();

            DiagnosisModeResponse quick = setMode(fixture, "QUICK");
            assertThat(quick.sensitivityStatus()).isEqualTo(SensitivityStatus.UNASSESSED);

            DiagnosisModeResponse detailed = setMode(fixture, "DETAILED");
            assertThat(detailed.sensitivityStatus()).isEqualTo(SensitivityStatus.HIGH);
        }

        @Test
        @DisplayName("알려준 다음 질문을 따라가면 끝까지 진행할 수 있다")
        void pointersLeadToCompletion() {
            Fixture fixture = answeredBasicQuestions();

            QuestionCode next = setMode(fixture, "DETAILED").nextQuestionCode();
            assertThat(next).isEqualTo(QuestionCode.SENSITIVITY);

            next = answer(fixture, next, "SENSITIVE_YES").nextQuestionCode();
            assertThat(next).isEqualTo(QuestionCode.CAUTION);

            next = answer(fixture, next, "FRAGRANCE", "ALCOHOL").nextQuestionCode();
            assertThat(next).isEqualTo(QuestionCode.EXPLORATION_HABIT);

            AnswerSaveResponse last = answer(fixture, next, "FREQUENTLY");
            assertThat(last.nextQuestionCode()).isNull();
            assertThat(last.nextAction()).isEqualTo(NextAction.READY_TO_COMPLETE);

            var completion = surveyService.complete(fixture.surveyId(), fixture.sessionToken());
            assertThat(completion.sensitivityStatus()).isEqualTo(SensitivityStatus.HIGH);
        }
    }

    @Nested
    @DisplayName("같은 값으로 다시 저장하면")
    class UnchangedAnswer {

        @Test
        @DisplayName("답변 행을 새로 만들지 않는다")
        void doesNotRecreateRows() {
            Fixture fixture = detailedWithCautionTwo();
            List<Long> before = answerIdsOf(fixture);

            answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL");

            assertThat(answerIdsOf(fixture)).containsExactlyElementsOf(before);
        }

        @Test
        @DisplayName("추천 재계산이 필요 없다고 알려준다")
        void reportsNoRecalculation() {
            Fixture fixture = detailedWithCautionTwo();

            AnswerSaveResponse response = answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL");

            assertThat(response.recommendationImpact()).isEqualTo(RecommendationImpact.NONE);
            assertThat(response.clearedQuestionCodes()).isEmpty();
        }

        @Test
        @DisplayName("다음 질문은 그대로 알려주므로 이동에 지장이 없다")
        void stillPointsToNextQuestion() {
            Fixture fixture = detailedWithCautionTwo();

            AnswerSaveResponse response = answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            assertThat(response.nextQuestionCode()).isEqualTo(QuestionCode.CAUTION);
            assertThat(response.nextAction()).isEqualTo(NextAction.ANSWER_QUESTION);
        }

        @Test
        @DisplayName("선택 순서만 다른 복수 선택도 같은 값으로 본다")
        void ignoresSelectionOrder() {
            Fixture fixture = detailedWithCautionTwo();
            List<Long> before = answerIdsOf(fixture);

            AnswerSaveResponse response = answer(fixture, QuestionCode.CAUTION, "ALCOHOL", "FRAGRANCE");

            assertThat(response.recommendationImpact()).isEqualTo(RecommendationImpact.NONE);
            assertThat(answerIdsOf(fixture)).containsExactlyElementsOf(before);
        }

        @Test
        @DisplayName("민감 여부를 같은 값으로 다시 저장해도 주의 요소와 민감도가 유지된다")
        void keepsCautionAndSensitivity() {
            Fixture fixture = detailedWithCautionTwo();

            AnswerSaveResponse response = answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            assertThat(response.derivedSensitivityStatus()).isEqualTo(SensitivityStatus.HIGH);
            assertThat(response.clearedQuestionCodes()).isEmpty();
            assertThat(progressSensitivityOf(fixture)).isEqualTo(SensitivityStatus.HIGH);
        }

        @Test
        @DisplayName("값이 바뀌면 정상 저장되고 재계산이 필요하다고 알려준다")
        void changedAnswerIsSavedNormally() {
            Fixture fixture = detailedWithCautionTwo();
            List<Long> before = answerIdsOf(fixture);

            AnswerSaveResponse response = answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE");

            assertThat(response.recommendationImpact()).isEqualTo(RecommendationImpact.RECALCULATE_REQUIRED);
            assertThat(response.derivedSensitivityStatus()).isEqualTo(SensitivityStatus.MEDIUM);
            assertThat(answerIdsOf(fixture)).isNotEqualTo(before);
        }
    }

    @Nested
    @DisplayName("이전 질문으로 돌아가면")
    class PreviousQuestion {

        @Test
        @DisplayName("복수 선택 질문은 최대 선택 개수를 함께 알려준다")
        void multipleSelectionCarriesMaxSelections() {
            Fixture fixture = detailedWithCautionTwo();

            var previous = surveyService.getPreviousQuestion(
                    fixture.surveyId(), QuestionCode.EXPLORATION_HABIT, fixture.sessionToken());

            assertThat(previous.questionCode()).isEqualTo(QuestionCode.CAUTION);
            assertThat(previous.selectionType()).isEqualTo(SelectionType.MULTIPLE);
            assertThat(previous.maxSelections()).isEqualTo(4);
        }

        @Test
        @DisplayName("단일 선택 질문은 최대 선택 개수가 1이다")
        void singleSelectionCarriesOne() {
            Fixture fixture = detailedWithCautionTwo();

            var previous = surveyService.getPreviousQuestion(
                    fixture.surveyId(), QuestionCode.CAUTION, fixture.sessionToken());

            assertThat(previous.questionCode()).isEqualTo(QuestionCode.SENSITIVITY);
            assertThat(previous.selectionType()).isEqualTo(SelectionType.SINGLE);
            assertThat(previous.maxSelections()).isEqualTo(1);
        }
    }

    // ---- fixtures & helpers ----

    private record Fixture(Long surveyId, String sessionToken) {
    }

    /** CONCERN/SKIN_TYPE까지 답한 상태 (진단 경로 미선택) */
    private Fixture answeredBasicQuestions() {
        String sessionToken = UUID.randomUUID().toString();
        sessionRepository.save(Session.create(sessionToken));
        Long surveyId = surveyService.create(sessionToken).id();
        Fixture fixture = new Fixture(surveyId, sessionToken);

        answer(fixture, QuestionCode.CONCERN, "MOISTURE");
        answer(fixture, QuestionCode.SKIN_TYPE, "DRY");
        return fixture;
    }

    /** 상세 진단 + 민감 '예' + 주의 요소 2개 = HIGH 상태 */
    private Fixture detailedWithCautionTwo() {
        Fixture fixture = answeredBasicQuestions();
        setMode(fixture, "DETAILED");
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
        answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL");
        return fixture;
    }

    private AnswerSaveResponse answer(Fixture fixture, QuestionCode questionCode, String... optionCodes) {
        return surveyService.saveAnswer(
                fixture.surveyId(), questionCode,
                new AnswerSaveRequest(List.of(optionCodes)), fixture.sessionToken());
    }

    private DiagnosisModeResponse setMode(Fixture fixture, String diagnosisMode) {
        return surveyService.setDiagnosisMode(
                fixture.surveyId(), new DiagnosisModeRequest(diagnosisMode), fixture.sessionToken());
    }

    private SensitivityStatus progressSensitivityOf(Fixture fixture) {
        return surveyService.getProgress(fixture.surveyId(), fixture.sessionToken()).sensitivityStatus();
    }

    private List<Long> answerIdsOf(Fixture fixture) {
        UserCondition condition = userConditionRepository.findById(fixture.surveyId()).orElseThrow();
        return userSurveyAnswerRepository.findAllByCondition(condition).stream()
                .map(UserSurveyAnswer::getId)
                .toList();
    }
}
