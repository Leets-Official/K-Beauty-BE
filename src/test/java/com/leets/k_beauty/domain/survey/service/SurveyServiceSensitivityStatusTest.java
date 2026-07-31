package com.leets.k_beauty.domain.survey.service;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.dto.SurveyProgressResponse;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SensitivityStatus;
import com.leets.k_beauty.global.exception.BusinessException;
import com.leets.k_beauty.global.exception.ErrorCode;
import com.leets.k_beauty.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 민감도 상태(sensitivityStatus) 산출 규칙 검증.
 *
 * <p>테스트마다 새 세션을 만들어 서로 간섭하지 않게 하고, @Transactional을 붙이지 않아
 * 실제 요청과 동일하게 서비스 호출마다 트랜잭션이 끊기도록 한다.
 */
@DisplayName("민감도 상태 산출")
class SurveyServiceSensitivityStatusTest extends IntegrationTestSupport {

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Nested
    @DisplayName("진단 경로 전환 시")
    class DiagnosisModeSwitch {

        @Test
        @DisplayName("QUICK으로 되돌리면 상세 진단 값이 남지 않고 UNASSESSED가 된다")
        void quickResetsToUnassessed() {
            Fixture fixture = detailedWithCautionTwo();

            setMode(fixture, "QUICK");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.UNASSESSED);
        }

        @Test
        @DisplayName("QUICK에서 DETAILED로 되돌리면 남아있는 답변으로 값이 복원된다")
        void detailedRestoresFromRemainingAnswers() {
            Fixture fixture = detailedWithCautionTwo();
            setMode(fixture, "QUICK");

            setMode(fixture, "DETAILED");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.HIGH);
        }

        @Test
        @DisplayName("QUICK으로 전환해도 기존 답변은 삭제되지 않는다")
        void quickKeepsAnswers() {
            Fixture fixture = detailedWithCautionTwo();

            setMode(fixture, "QUICK");

            assertThat(answerOf(fixture, QuestionCode.CAUTION))
                    .containsExactlyInAnyOrder("FRAGRANCE", "ALCOHOL");
            assertThat(answerOf(fixture, QuestionCode.SENSITIVITY))
                    .containsExactly("SENSITIVE_YES");
        }

        @Test
        @DisplayName("LOW도 QUICK 전환 시 UNASSESSED가 되고 DETAILED로 되돌리면 복원된다")
        void lowIsDistinguishedFromUnassessed() {
            Fixture fixture = startDetailed();
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");
            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.LOW);

            setMode(fixture, "QUICK");
            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.UNASSESSED);

            setMode(fixture, "DETAILED");
            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.LOW);
        }

        @Test
        @DisplayName("신규 흐름에서는 QUICK/DETAILED 모두 초기값 UNASSESSED를 유지한다")
        void newSurveyKeepsInitialValue() {
            Fixture quick = startWithBasicAnswers();
            setMode(quick, "QUICK");
            assertThat(sensitivityOf(quick)).isEqualTo(SensitivityStatus.UNASSESSED);

            Fixture detailed = startWithBasicAnswers();
            setMode(detailed, "DETAILED");
            assertThat(sensitivityOf(detailed)).isEqualTo(SensitivityStatus.UNASSESSED);
        }
    }

    @Nested
    @DisplayName("답변 저장 시")
    class AnswerSave {

        @Test
        @DisplayName("민감 여부가 '아니요'면 LOW")
        void noMeansLow() {
            Fixture fixture = startDetailed();

            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.LOW);
        }

        @Test
        @DisplayName("'예'인데 주의 요소를 아직 고르지 않았으면 UNASSESSED")
        void yesWithoutCautionMeansUnassessed() {
            Fixture fixture = startDetailed();

            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.UNASSESSED);
        }

        @Test
        @DisplayName("'예' + 주의 요소 1개면 MEDIUM")
        void yesWithOneCautionMeansMedium() {
            Fixture fixture = startDetailed();
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            answer(fixture, QuestionCode.CAUTION, "FRAGRANCE");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.MEDIUM);
        }

        @Test
        @DisplayName("'예' + 주의 요소 2개면 HIGH")
        void yesWithTwoCautionsMeansHigh() {
            Fixture fixture = startDetailed();
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.HIGH);
        }

        @Test
        @DisplayName("'예' + '잘 모르겠어요' 단독이면 MEDIUM")
        void yesWithUnknownOnlyMeansMedium() {
            Fixture fixture = startDetailed();
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            answer(fixture, QuestionCode.CAUTION, "UNKNOWN");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.MEDIUM);
        }

        @Test
        @DisplayName("주의 요소를 2개에서 1개로 줄이면 HIGH에서 MEDIUM으로 내려간다")
        void reducingCautionLowersStatus() {
            Fixture fixture = detailedWithCautionTwo();

            answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.MEDIUM);
        }

        @Test
        @DisplayName("'예'에서 '아니요'로 바꾸면 주의 요소가 삭제되고 LOW가 된다")
        void changingYesToNoClearsCaution() {
            Fixture fixture = detailedWithCautionTwo();

            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.LOW);
            assertThat(answerOf(fixture, QuestionCode.CAUTION)).isEmpty();
        }

        @Test
        @DisplayName("'아니요'에서 '예'로 되돌리면 주의 요소가 비어 있어 UNASSESSED가 된다")
        void changingNoToYesResetsToUnassessed() {
            Fixture fixture = detailedWithCautionTwo();
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");

            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.UNASSESSED);
        }

        @Test
        @DisplayName("탐색 습관을 바꿔도 민감도는 영향을 받지 않는다")
        void explorationHabitDoesNotAffectSensitivity() {
            Fixture fixture = detailedWithCautionTwo();

            answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");
            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.HIGH);

            answer(fixture, QuestionCode.EXPLORATION_HABIT, "RARELY");
            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.HIGH);
        }

        @Test
        @DisplayName("주의 요소를 최대치(4개)로 골라도 HIGH")
        void maxCautionsMeansHigh() {
            Fixture fixture = startDetailed();
            answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");

            answer(fixture, QuestionCode.CAUTION,
                    "FRAGRANCE", "ALCOHOL", "OILY_TEXTURE", "EXFOLIATION");

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.HIGH);
        }

        @Test
        @DisplayName("배타 선택지 위반으로 저장이 실패하면 기존 답변과 민감도가 그대로 유지된다")
        void failedSaveDoesNotCorruptState() {
            Fixture fixture = detailedWithCautionTwo();

            assertThatThrownBy(() ->
                    answer(fixture, QuestionCode.CAUTION, "UNKNOWN", "FRAGRANCE"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(thrown -> ((BusinessException) thrown).getErrorCode())
                    .isEqualTo(ErrorCode.EXCLUSIVE_OPTION_VIOLATION);

            assertThat(sensitivityOf(fixture)).isEqualTo(SensitivityStatus.HIGH);
            assertThat(answerOf(fixture, QuestionCode.CAUTION))
                    .containsExactlyInAnyOrder("FRAGRANCE", "ALCOHOL");
        }
    }

    @Nested
    @DisplayName("설문 완료 시")
    class Completion {

        @Test
        @DisplayName("상세 진단으로 답한 뒤 QUICK으로 되돌려 완료하면 UNASSESSED가 실린다")
        void quickCompletionCarriesUnassessed() {
            Fixture fixture = detailedWithCautionTwo();
            answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");
            setMode(fixture, "QUICK");

            var completion = surveyService.complete(fixture.surveyId(), fixture.sessionToken());

            assertThat(completion.diagnosisMode().name()).isEqualTo("QUICK");
            assertThat(completion.sensitivityStatus()).isEqualTo(SensitivityStatus.UNASSESSED);
        }

        @Test
        @DisplayName("상세 진단으로 완주하면 산출된 민감도가 그대로 실린다")
        void detailedCompletionCarriesDerivedStatus() {
            Fixture fixture = detailedWithCautionTwo();
            answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");

            var completion = surveyService.complete(fixture.surveyId(), fixture.sessionToken());

            assertThat(completion.diagnosisMode().name()).isEqualTo("DETAILED");
            assertThat(completion.sensitivityStatus()).isEqualTo(SensitivityStatus.HIGH);
        }
    }

    @Nested
    @DisplayName("처음부터 다시 하기")
    class Restart {

        @Test
        @DisplayName("새 설문 컨텍스트는 이전 민감도를 물려받지 않는다")
        void restartResetsSensitivity() {
            Fixture previous = detailedWithCautionTwo();
            assertThat(sensitivityOf(previous)).isEqualTo(SensitivityStatus.HIGH);

            Long newSurveyId = surveyService
                    .restart(previous.surveyId(), previous.sessionToken())
                    .surveyResponseId();
            Fixture restarted = new Fixture(newSurveyId, previous.sessionToken());

            assertThat(sensitivityOf(restarted)).isEqualTo(SensitivityStatus.UNASSESSED);
            assertThat(answerOf(restarted, QuestionCode.CAUTION)).isEmpty();
            assertThat(answerOf(restarted, QuestionCode.SENSITIVITY)).isEmpty();
        }
    }

    // ---- fixtures & helpers ----

    private record Fixture(Long surveyId, String sessionToken) {
    }

    /** 새 세션으로 설문을 시작하고 CONCERN/SKIN_TYPE까지 답한 상태 */
    private Fixture startWithBasicAnswers() {
        String sessionToken = sessionRepository.save(Session.create()).getSessionToken();
        Long surveyId = surveyService.create(sessionToken).id();
        Fixture fixture = new Fixture(surveyId, sessionToken);

        answer(fixture, QuestionCode.CONCERN, "MOISTURE");
        answer(fixture, QuestionCode.SKIN_TYPE, "DRY");
        return fixture;
    }

    /** 위 상태에서 상세 진단까지 선택한 상태 */
    private Fixture startDetailed() {
        Fixture fixture = startWithBasicAnswers();
        setMode(fixture, "DETAILED");
        return fixture;
    }

    /** 상세 진단 + 민감 '예' + 주의 요소 2개 = HIGH 상태 */
    private Fixture detailedWithCautionTwo() {
        Fixture fixture = startDetailed();
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
        answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL");
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

    private SensitivityStatus sensitivityOf(Fixture fixture) {
        return progressOf(fixture).sensitivityStatus();
    }

    private List<String> answerOf(Fixture fixture, QuestionCode questionCode) {
        return progressOf(fixture).answers().stream()
                .filter(answered -> answered.questionCode() == questionCode)
                .findFirst()
                .map(SurveyProgressResponse.AnsweredQuestion::optionCodes)
                .orElseGet(List::of);
    }

    private SurveyProgressResponse progressOf(Fixture fixture) {
        return surveyService.getProgress(fixture.surveyId(), fixture.sessionToken());
    }
}
