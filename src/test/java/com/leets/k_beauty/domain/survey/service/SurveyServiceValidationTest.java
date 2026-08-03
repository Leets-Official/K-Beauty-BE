package com.leets.k_beauty.domain.survey.service;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.global.exception.BusinessException;
import com.leets.k_beauty.global.exception.ErrorCode;
import com.leets.k_beauty.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 사용자의 잘못된 입력과 권한 없는 접근에 대한 처리 검증.
 */
@DisplayName("설문 입력 검증")
class SurveyServiceValidationTest extends IntegrationTestSupport {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Nested
    @DisplayName("답변 저장")
    class SaveAnswer {

        @Test
        @DisplayName("단일 선택 질문에 두 개를 고르면 거부한다")
        void rejectsMultipleOnSingleSelection() {
            Fixture fixture = newSurvey();

            assertBusinessError(
                    () -> answer(fixture, QuestionCode.CONCERN, "MOISTURE", "TONE"),
                    ErrorCode.SINGLE_SELECTION_VIOLATION);
        }

        @Test
        @DisplayName("복수 선택 질문에 최대 개수를 넘기면 거부한다")
        void rejectsTooManySelections() {
            Fixture fixture = readyForCaution();

            assertBusinessError(
                    () -> answer(fixture, QuestionCode.CAUTION,
                            "FRAGRANCE", "ALCOHOL", "OILY_TEXTURE", "EXFOLIATION", "UNKNOWN"),
                    ErrorCode.MAX_SELECTION_EXCEEDED);
        }

        @Test
        @DisplayName("존재하지 않는 선택지는 거부한다")
        void rejectsUnknownOption() {
            Fixture fixture = newSurvey();

            assertBusinessError(
                    () -> answer(fixture, QuestionCode.CONCERN, "NOT_A_REAL_OPTION"),
                    ErrorCode.SURVEY_OPTION_INVALID);
        }

        @Test
        @DisplayName("'잘 모르겠어요'는 다른 항목과 함께 고를 수 없다")
        void rejectsExclusiveWithOthers() {
            Fixture fixture = readyForCaution();

            assertBusinessError(
                    () -> answer(fixture, QuestionCode.CAUTION, "UNKNOWN", "FRAGRANCE"),
                    ErrorCode.EXCLUSIVE_OPTION_VIOLATION);
        }

        @Test
        @DisplayName("민감 여부에 '예'로 답하지 않았으면 주의 요소를 저장할 수 없다")
        void rejectsCautionWithoutSensitiveYes() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");

            assertBusinessError(
                    () -> answer(fixture, QuestionCode.CAUTION, "FRAGRANCE"),
                    ErrorCode.CAUTION_PREREQUISITE_MISSING);
        }
    }

    @Nested
    @DisplayName("진단 경로 선택")
    class DiagnosisMode {

        @Test
        @DisplayName("정해진 값이 아니면 거부한다")
        void rejectsUnknownMode() {
            Fixture fixture = answeredBasicQuestions();

            assertBusinessError(
                    () -> setMode(fixture, "FAST"),
                    ErrorCode.INVALID_DIAGNOSIS_MODE);
        }

        @Test
        @DisplayName("고민과 피부 타입을 먼저 답하지 않으면 고를 수 없다")
        void rejectsBeforeBasicAnswers() {
            Fixture fixture = newSurvey();

            assertBusinessError(
                    () -> setMode(fixture, "QUICK"),
                    ErrorCode.DIAGNOSIS_MODE_PREREQUISITE_MISSING);
        }
    }

    @Nested
    @DisplayName("설문 완료")
    class Completion {

        @Test
        @DisplayName("상세 진단인데 필수 응답이 남아 있으면 완료할 수 없다")
        void rejectsWhenRequiredAnswersMissing() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");

            assertBusinessError(
                    () -> surveyService.complete(fixture.surveyId(), fixture.sessionToken()),
                    ErrorCode.SURVEY_ANSWER_MISSING);
        }

        @Test
        @DisplayName("민감 '예' 뒤에 주의 요소를 답하지 않으면 완료할 수 없다")
        void rejectsWhenCautionMissing() {
            Fixture fixture = answeredBasicQuestions();
            setMode(fixture, "DETAILED");
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
            answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");

            assertBusinessError(
                    () -> surveyService.complete(fixture.surveyId(), fixture.sessionToken()),
                    ErrorCode.SURVEY_ANSWER_MISSING);
        }
    }

    @Nested
    @DisplayName("접근 제어")
    class AccessControl {

        @Test
        @DisplayName("모르는 세션 토큰으로는 조회할 수 없다")
        void rejectsUnknownSession() {
            assertBusinessError(
                    () -> surveyService.getCurrent(UUID.randomUUID().toString()),
                    ErrorCode.SESSION_NOT_FOUND);
        }

        @Test
        @DisplayName("없는 설문은 조회할 수 없다")
        void rejectsUnknownSurvey() {
            String sessionToken = newSessionToken();

            assertBusinessError(
                    () -> surveyService.getProgress(999_999L, sessionToken),
                    ErrorCode.SURVEY_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 사람의 설문은 조회할 수 없다")
        void rejectsOtherSessionSurvey() {
            Fixture mine = newSurvey();
            String otherToken = newSessionToken();

            assertBusinessError(
                    () -> surveyService.getProgress(mine.surveyId(), otherToken),
                    ErrorCode.SURVEY_FORBIDDEN);
        }

        @Test
        @DisplayName("다른 사람의 설문에는 답변할 수 없다")
        void rejectsAnsweringOtherSessionSurvey() {
            Fixture mine = newSurvey();
            String otherToken = newSessionToken();
            Fixture spoofed = new Fixture(mine.surveyId(), otherToken);

            assertBusinessError(
                    () -> answer(spoofed, QuestionCode.CONCERN, "MOISTURE"),
                    ErrorCode.SURVEY_FORBIDDEN);
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

    /** 주의 요소를 저장할 수 있는 상태 (상세 진단 + 민감 '예') */
    private Fixture readyForCaution() {
        Fixture fixture = answeredBasicQuestions();
        setMode(fixture, "DETAILED");
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
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

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
                .isEqualTo(expected);
    }
}
