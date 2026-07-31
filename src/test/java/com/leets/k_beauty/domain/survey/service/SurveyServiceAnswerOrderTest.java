package com.leets.k_beauty.domain.survey.service;

import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.dto.SurveyProgressResponse;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 답변 목록(answers[])이 설문 흐름 순서를 유지하는지 검증.
 *
 * <p>saveAnswer가 delete-then-insert 방식이라 답변을 다시 저장하면 그 행이 가장 뒤로 밀린다.
 * 저장 순서가 아니라 surveyStep 순서로 내려가야 클라이언트가 화면 순서로 신뢰할 수 있다.
 */
@DisplayName("답변 목록 정렬")
class SurveyServiceAnswerOrderTest extends IntegrationTestSupport {

    private static final List<QuestionCode> FLOW_ORDER = List.of(
            QuestionCode.CONCERN,
            QuestionCode.SKIN_TYPE,
            QuestionCode.SENSITIVITY,
            QuestionCode.CAUTION,
            QuestionCode.EXPLORATION_HABIT);

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    @DisplayName("설문을 순서대로 답하면 answers[]도 설문 흐름 순서로 나온다")
    void answersFollowFlowOrder() {
        Fixture fixture = answeredAllQuestions();

        assertThat(answeredCodesOf(fixture)).containsExactlyElementsOf(FLOW_ORDER);
    }

    @Test
    @DisplayName("중간 질문을 다시 저장해도 answers[] 순서가 유지된다")
    void reSavingMiddleQuestionKeepsOrder() {
        Fixture fixture = answeredAllQuestions();

        // 뒤로 가서 Q4의 답을 바꾸면 해당 행이 새로 생성되어 가장 뒤로 밀림
        answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE", "EXFOLIATION");

        assertThat(answeredCodesOf(fixture)).containsExactlyElementsOf(FLOW_ORDER);
    }

    @Test
    @DisplayName("여러 질문을 역순으로 다시 저장해도 answers[] 순서가 유지된다")
    void reSavingInReverseKeepsOrder() {
        Fixture fixture = answeredAllQuestions();

        answer(fixture, QuestionCode.EXPLORATION_HABIT, "RARELY");
        answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE");
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
        answer(fixture, QuestionCode.SKIN_TYPE, "OILY");
        answer(fixture, QuestionCode.CONCERN, "TROUBLE");

        assertThat(answeredCodesOf(fixture)).containsExactlyElementsOf(FLOW_ORDER);
    }

    @Test
    @DisplayName("다중 선택 답변이 있어도 조회할 때마다 같은 순서가 나온다")
    void orderIsStableAcrossCalls() {
        Fixture fixture = answeredAllQuestions();
        answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL", "OILY_TEXTURE");

        List<QuestionCode> first = answeredCodesOf(fixture);
        List<QuestionCode> second = answeredCodesOf(fixture);

        assertThat(first).containsExactlyElementsOf(FLOW_ORDER);
        assertThat(second).containsExactlyElementsOf(first);
    }

    @Test
    @DisplayName("설문을 막 시작해 답변이 하나도 없으면 answers[]는 비어 있다")
    void emptyAnswersAreHandled() {
        Fixture fixture = newSurvey();

        assertThat(answeredCodesOf(fixture)).isEmpty();
    }

    @Test
    @DisplayName("빠른 진단으로 2문항만 답한 경우에도 흐름 순서를 따른다")
    void quickPathKeepsFlowOrder() {
        Fixture fixture = newSurvey();
        answer(fixture, QuestionCode.CONCERN, "MOISTURE");
        answer(fixture, QuestionCode.SKIN_TYPE, "DRY");
        setMode(fixture, "QUICK");

        assertThat(answeredCodesOf(fixture))
                .containsExactly(QuestionCode.CONCERN, QuestionCode.SKIN_TYPE);
    }

    @Test
    @DisplayName("민감 '아니요'로 CAUTION을 건너뛴 경우에도 흐름 순서를 따른다")
    void skippedCautionKeepsFlowOrder() {
        Fixture fixture = newSurvey();
        answer(fixture, QuestionCode.CONCERN, "MOISTURE");
        answer(fixture, QuestionCode.SKIN_TYPE, "DRY");
        setMode(fixture, "DETAILED");
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");
        answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");

        assertThat(answeredCodesOf(fixture)).containsExactly(
                QuestionCode.CONCERN,
                QuestionCode.SKIN_TYPE,
                QuestionCode.SENSITIVITY,
                QuestionCode.EXPLORATION_HABIT);
    }

    @Test
    @DisplayName("'예'에서 '아니요'로 바꿔 CAUTION이 삭제된 뒤에도 흐름 순서를 따른다")
    void clearedCautionKeepsFlowOrder() {
        Fixture fixture = answeredAllQuestions();

        // Q3을 '아니요'로 바꾸면 CAUTION 답변이 삭제되고, Q3 행은 새로 저장되어 가장 뒤로 밀린다
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_NO");

        assertThat(answeredCodesOf(fixture)).containsExactly(
                QuestionCode.CONCERN,
                QuestionCode.SKIN_TYPE,
                QuestionCode.SENSITIVITY,
                QuestionCode.EXPLORATION_HABIT);
    }

    @Test
    @DisplayName("재저장 후에도 이전 질문 조회가 흐름 순서를 따른다")
    void previousQuestionFollowsFlowOrder() {
        Fixture fixture = answeredAllQuestions();
        answer(fixture, QuestionCode.CAUTION, "OILY_TEXTURE");

        var previous = surveyService.getPreviousQuestion(
                fixture.surveyId(), QuestionCode.EXPLORATION_HABIT, fixture.sessionToken());

        assertThat(previous.questionCode()).isEqualTo(QuestionCode.CAUTION);
    }

    // ---- fixtures & helpers ----

    private record Fixture(Long surveyId, String sessionToken) {
    }

    /** 새 세션으로 설문만 시작한 상태 (답변 없음) */
    private Fixture newSurvey() {
        String sessionToken = sessionRepository.save(Session.create()).getSessionToken();
        Long surveyId = surveyService.create(sessionToken).id();
        return new Fixture(surveyId, sessionToken);
    }

    /** 상세 진단으로 5문항을 모두 답한 상태 */
    private Fixture answeredAllQuestions() {
        Fixture fixture = newSurvey();

        answer(fixture, QuestionCode.CONCERN, "MOISTURE");
        answer(fixture, QuestionCode.SKIN_TYPE, "DRY");
        setMode(fixture, "DETAILED");
        answer(fixture, QuestionCode.SENSITIVITY, "SENSITIVE_YES");
        answer(fixture, QuestionCode.CAUTION, "FRAGRANCE", "ALCOHOL");
        answer(fixture, QuestionCode.EXPLORATION_HABIT, "FREQUENTLY");
        return fixture;
    }

    private void setMode(Fixture fixture, String diagnosisMode) {
        surveyService.setDiagnosisMode(
                fixture.surveyId(), new DiagnosisModeRequest(diagnosisMode), fixture.sessionToken());
    }

    private void answer(Fixture fixture, QuestionCode questionCode, String... optionCodes) {
        surveyService.saveAnswer(
                fixture.surveyId(), questionCode,
                new AnswerSaveRequest(List.of(optionCodes)), fixture.sessionToken());
    }

    private List<QuestionCode> answeredCodesOf(Fixture fixture) {
        return surveyService.getProgress(fixture.surveyId(), fixture.sessionToken())
                .answers().stream()
                .map(SurveyProgressResponse.AnsweredQuestion::questionCode)
                .toList();
    }
}
