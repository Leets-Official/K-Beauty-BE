package com.leets.k_beauty.domain.survey.service;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.enums.NextAction;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;
import com.leets.k_beauty.domain.survey.repository.UserConditionRepository;
import com.leets.k_beauty.global.exception.BusinessException;
import com.leets.k_beauty.global.exception.ErrorCode;
import com.leets.k_beauty.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 설문 시작부터 완료·재시작까지의 흐름 검증 
 */
@DisplayName("설문 흐름")
class SurveyServiceFlowTest extends IntegrationTestSupport {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserConditionRepository userConditionRepository;

    @Nested
    @DisplayName("설문 시작과 재진입")
    class StartAndResume {

        @Test
        @DisplayName("새 설문은 진행 중 상태로 시작한다")
        void newSurveyStartsInProgress() {
            String sessionToken = newSessionToken();

            var created = surveyService.create(sessionToken);

            assertThat(created.status()).isEqualTo(SurveyStatus.IN_PROGRESS);
            assertThat(created.diagnosisMode()).isNull();
        }

        @Test
        @DisplayName("진행 중인 설문이 있으면 새로 만들 수 없다")
        void cannotCreateWhileInProgress() {
            String sessionToken = newSessionToken();
            surveyService.create(sessionToken);

            assertThatThrownBy(() -> surveyService.create(sessionToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
                    .isEqualTo(ErrorCode.SURVEY_IN_PROGRESS_EXISTS);
        }

        @Test
        @DisplayName("앱을 다시 열면 진행 중이던 설문을 그대로 돌려준다")
        void resumeReturnsInProgressSurvey() {
            Fixture fixture = answeredBasicQuestions();

            var current = surveyService.getCurrent(fixture.sessionToken());

            assertThat(current.surveyResponseId()).isEqualTo(fixture.surveyId());
            assertThat(current.status()).isEqualTo(SurveyStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("완료한 설문도 재진입하면 조회된다")
        void resumeReturnsCompletedSurvey() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "QUICK");
            surveyService.complete(fixture.surveyId(), fixture.sessionToken());

            var current = surveyService.getCurrent(fixture.sessionToken());

            assertThat(current.surveyResponseId()).isEqualTo(fixture.surveyId());
            assertThat(current.status()).isEqualTo(SurveyStatus.COMPLETED);
        }

        @Test
        @DisplayName("설문을 시작한 적이 없으면 조회되지 않는다")
        void resumeFailsWhenNeverStarted() {
            String sessionToken = newSessionToken();

            assertThatThrownBy(() -> surveyService.getCurrent(sessionToken))
                    .isInstanceOf(BusinessException.class)
                    .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
                    .isEqualTo(ErrorCode.SURVEY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("빠른 진단")
    class QuickDiagnosis {

        @Test
        @DisplayName("2문항만 답하고 완료할 수 있다")
        void completesWithTwoAnswers() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "QUICK");

            var completion = surveyService.complete(fixture.surveyId(), fixture.sessionToken());

            assertThat(completion.status()).isEqualTo(SurveyStatus.COMPLETED);
            assertThat(completion.completedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("피부 타입 중립 모드")
    class TypeNeutralMode {

        @Test
        @DisplayName("'잘 모르겠어요'를 고르면 타입 중립 모드가 켜진다")
        void unknownSkinTypeTurnsOnNeutralMode() {
            Fixture fixture = newSurvey();
            answer(fixture, QuestionCode.CONCERN, "MOISTURE");

            answer(fixture, QuestionCode.SKIN_TYPE, "UNKNOWN");

            assertThat(progressOf(fixture).typeNeutralMode()).isTrue();
        }

        @Test
        @DisplayName("피부 타입을 다시 고르면 타입 중립 모드가 꺼진다")
        void choosingTypeTurnsOffNeutralMode() {
            Fixture fixture = newSurvey();
            answer(fixture, QuestionCode.CONCERN, "MOISTURE");
            answer(fixture, QuestionCode.SKIN_TYPE, "UNKNOWN");

            answer(fixture, QuestionCode.SKIN_TYPE, "COMBINATION");

            assertThat(progressOf(fixture).typeNeutralMode()).isFalse();
        }
    }

    @Nested
    @DisplayName("뒤로 가기")
    class GoBack {

        @Test
        @DisplayName("연속으로 뒤로 가면 첫 질문까지 되돌아간다")
        void goesBackStepByStep() {
            Fixture fixture = answeredAllQuestions();

            assertThat(previousOf(fixture, QuestionCode.SENSITIVITY).questionCode())
                    .isEqualTo(QuestionCode.SKIN_TYPE);
            assertThat(previousOf(fixture, QuestionCode.SKIN_TYPE).questionCode())
                    .isEqualTo(QuestionCode.CONCERN);
        }

        @Test
        @DisplayName("첫 질문에서 뒤로 가면 오류 대신 온보딩으로 안내한다")
        void goingBackFromFirstQuestionLeadsToOnboarding() {
            Fixture fixture = answeredAllQuestions();

            var previous = previousOf(fixture, QuestionCode.CONCERN);

            assertThat(previous.nextAction()).isEqualTo(NextAction.GO_TO_ONBOARDING);
            assertThat(previous.questionCode()).isNull();
            assertThat(previous.options()).isNull();
        }

        @Test
        @DisplayName("답변이 하나도 없을 때 뒤로 가도 온보딩으로 안내한다")
        void goingBackWithoutAnyAnswerLeadsToOnboarding() {
            Fixture fixture = newSurvey();

            var previous = previousOf(fixture, QuestionCode.CONCERN);

            assertThat(previous.nextAction()).isEqualTo(NextAction.GO_TO_ONBOARDING);
        }

        @Test
        @DisplayName("이전 질문으로 돌아갈 때는 다음 행동이 질문 답하기다")
        void goingBackToAnsweredQuestionAsksToAnswer() {
            Fixture fixture = answeredAllQuestions();

            var previous = previousOf(fixture, QuestionCode.SENSITIVITY);

            assertThat(previous.nextAction()).isEqualTo(NextAction.ANSWER_QUESTION);
            assertThat(previous.questionCode()).isEqualTo(QuestionCode.SKIN_TYPE);
        }

        @Test
        @DisplayName("이전에 고른 답이 프리필로 함께 온다")
        void carriesPreviousSelection() {
            Fixture fixture = answeredAllQuestions();

            var previous = previousOf(fixture, QuestionCode.EXPLORATION_HABIT);

            assertThat(previous.questionCode()).isEqualTo(QuestionCode.CAUTION);
            assertThat(previous.selectedOptionCodes())
                    .containsExactlyInAnyOrder("FRAGRANCE", "ALCOHOL");
        }

        @Test
        @DisplayName("민감 '아니요'였다면 주의 요소를 건너뛰고 민감 여부로 돌아간다")
        void skipsCautionWhenNotSensitive() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");
            answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");

            var previous = previousOf(fixture, QuestionCode.EXPLORATION_HABIT);

            assertThat(previous.questionCode()).isEqualTo(QuestionCode.SENSITIVITY);
            assertThat(previous.selectedOptionCodes()).containsExactly("SENSITIVE_NO");
        }

        @Test
        @DisplayName("아직 답하지 않은 질문에서 뒤로 가면 마지막으로 답한 질문을 준다")
        void fallsBackWhenCurrentQuestionUnanswered() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");

            // 민감 여부 화면이 막 떴을 뿐 아직 답하지 않은 상태
            var previous = previousOf(fixture, QuestionCode.SENSITIVITY);

            assertThat(previous.questionCode()).isEqualTo(QuestionCode.SKIN_TYPE);
        }
    }

    @Nested
    @DisplayName("처음부터 다시 하기")
    class Restart {

        @Test
        @DisplayName("이전 설문은 중단되고 새 설문이 첫 질문부터 시작한다")
        void previousIsAbandonedAndNewOneStarts() {
            Fixture fixture = answeredAllQuestions();

            var restarted = surveyService.restart(fixture.surveyId(), fixture.sessionToken());

            assertThat(restarted.previousSurveyResponseId()).isEqualTo(fixture.surveyId());
            assertThat(restarted.status()).isEqualTo(SurveyStatus.IN_PROGRESS);
            assertThat(restarted.currentQuestionCode()).isEqualTo(QuestionCode.CONCERN);
            assertThat(restarted.currentStep()).isEqualTo(1);
            assertThat(userConditionRepository.findById(fixture.surveyId()).orElseThrow().getStatus())
                    .isEqualTo(SurveyStatus.ABANDONED);
        }

        @Test
        @DisplayName("세션은 유지되므로 재시작 후에도 새 설문 생성은 막힌다")
        void createIsStillBlockedAfterRestart() {
            Fixture fixture = answeredAllQuestions();
            surveyService.restart(fixture.surveyId(), fixture.sessionToken());

            assertThatThrownBy(() -> surveyService.create(fixture.sessionToken()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
                    .isEqualTo(ErrorCode.SURVEY_IN_PROGRESS_EXISTS);
        }
    }

    @Nested
    @DisplayName("추천 화면에서 뒤로 와서")
    class AfterCompletion {

        @Test
        @DisplayName("답을 바꾸면 다시 진행 중 상태가 된다")
        void changingAnswerReopensSurvey() {
            Fixture fixture = completedQuickSurvey();

            answer(fixture, QuestionCode.CONCERN, "TROUBLE");

            assertThat(progressOf(fixture).status()).isEqualTo(SurveyStatus.IN_PROGRESS);
            assertThat(userConditionRepository.findById(fixture.surveyId()).orElseThrow().getCompletedAt())
                    .isNull();
        }

        @Test
        @DisplayName("답을 바꾸지 않고 넘기면 완료 상태가 유지된다")
        void movingWithoutChangeKeepsCompleted() {
            Fixture fixture = completedQuickSurvey();

            answer(fixture, QuestionCode.CONCERN, "MOISTURE");

            assertThat(progressOf(fixture).status()).isEqualTo(SurveyStatus.COMPLETED);
        }

        @Test
        @DisplayName("탐색 습관만 바꾸면 추천에 영향이 없어 완료 상태가 유지된다")
        void changingExplorationHabitKeepsCompleted() {
            Fixture fixture = completedDetailedSurvey();

            answer(fixture, QuestionCode.EXPLORATION_HABIT, "RARELY");

            assertThat(progressOf(fixture).status()).isEqualTo(SurveyStatus.COMPLETED);
        }

        @Test
        @DisplayName("진단 경로를 바꾸면 다시 진행 중 상태가 된다")
        void changingDiagnosisModeReopensSurvey() {
            Fixture fixture = completedQuickSurvey();

            setMode(fixture, "DETAILED");

            assertThat(progressOf(fixture).status()).isEqualTo(SurveyStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("같은 진단 경로를 다시 고르면 완료 상태가 유지된다")
        void reselectingSameDiagnosisModeKeepsCompleted() {
            Fixture fixture = completedQuickSurvey();

            setMode(fixture, "QUICK");

            assertThat(progressOf(fixture).status()).isEqualTo(SurveyStatus.COMPLETED);
        }

        @Test
        @DisplayName("답을 바꾼 뒤 다시 완료할 수 있다")
        void canCompleteAgainAfterChange() {
            Fixture fixture = completedQuickSurvey();
            answer(fixture, QuestionCode.CONCERN, "TROUBLE");

            var completion = surveyService.complete(fixture.surveyId(), fixture.sessionToken());

            assertThat(completion.status()).isEqualTo(SurveyStatus.COMPLETED);
            assertThat(completion.completedAt()).isNotNull();
        }

        @Test
        @DisplayName("바뀐 게 없는데 완료를 다시 호출해도 처음 완료 상태를 그대로 돌려준다")
        void completingAgainWithoutChangeKeepsFirstResult() {
            Fixture fixture = completedQuickSurvey();
            LocalDateTime firstCompletedAt = userConditionRepository.findById(fixture.surveyId())
                    .orElseThrow().getCompletedAt();

            var completion = surveyService.complete(fixture.surveyId(), fixture.sessionToken());

            assertThat(completion.status()).isEqualTo(SurveyStatus.COMPLETED);
            assertThat(completion.completedAt()).isEqualTo(firstCompletedAt);
        }
    }

    // ---- fixtures & helpers ----

    private record Fixture(Long surveyId, String sessionToken) {
    }

    private String newSessionToken() {
        String rawToken = UUID.randomUUID().toString();
        sessionRepository.save(Session.create(rawToken));
        return rawToken;
    }

    private Fixture newSurvey() {
        String sessionToken = newSessionToken();
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

    private Fixture completedQuickSurvey() {
        Fixture fixture = answeredBasicQuestions();
        setMode(fixture, "QUICK");
        surveyService.complete(fixture.surveyId(), fixture.sessionToken());
        return fixture;
    }

    private Fixture completedDetailedSurvey() {
        Fixture fixture = answeredAllQuestions();
        surveyService.complete(fixture.surveyId(), fixture.sessionToken());
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

    private com.leets.k_beauty.domain.survey.dto.SurveyProgressResponse progressOf(Fixture fixture) {
        return surveyService.getProgress(fixture.surveyId(), fixture.sessionToken());
    }

    private com.leets.k_beauty.domain.survey.dto.PreviousQuestionResponse previousOf(Fixture fixture, QuestionCode from) {
        return surveyService.getPreviousQuestion(fixture.surveyId(), from, fixture.sessionToken());
    }
}
