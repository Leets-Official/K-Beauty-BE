package com.leets.k_beauty.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.leets.k_beauty.domain.recommendation.dto.CandidateSelectRequest;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationCandidateResponse;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationResponse;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationStepResponse;
import com.leets.k_beauty.domain.recommendation.repository.RecommendationCandidateRepository;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.dto.AnswerSaveRequest;
import com.leets.k_beauty.domain.survey.dto.DiagnosisModeRequest;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.service.SurveyService;
import com.leets.k_beauty.support.IntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("추천 후보 사용자 선택 상태")
class RecommendationServiceUserSelectionTest extends IntegrationTestSupport {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RecommendationCandidateRepository candidateRepository;

    @Test
    @DisplayName("추천 생성 시 기본 선택 후보만 사용자 선택 상태가 된다")
    void generatedSelectedCandidatesAreUserSelected() {
        Fixture fixture = completedQuickSurvey();

        RecommendationResponse response = recommendationService.generate(fixture.sessionToken());

        response.steps().forEach(step -> assertOnlySelectedCandidateMarked(step, step.selected().candidateId()));
        response.steps().stream()
                .map(step -> step.selected().candidateId())
                .forEach(candidateId -> assertThat(candidateRepository.findById(candidateId).orElseThrow().isUserSelected())
                        .isTrue());
        response.steps().stream()
                .flatMap(step -> step.others().stream())
                .map(RecommendationCandidateResponse::candidateId)
                .forEach(candidateId -> assertThat(candidateRepository.findById(candidateId).orElseThrow().isUserSelected())
                        .isFalse());
    }

    @Test
    @DisplayName("후보 상품을 교체하면 선택한 후보만 사용자 선택 상태가 된다")
    void selectedCandidateIsMarkedAsUserSelected() {
        Fixture fixture = completedQuickSurvey();
        RecommendationResponse generated = recommendationService.generate(fixture.sessionToken());
        RecommendationStepResponse firstStep = generated.steps().get(0);
        RecommendationCandidateResponse initialSelected = firstStep.selected();
        RecommendationCandidateResponse firstReplacement = firstStep.others().get(0);

        RecommendationResponse changed = recommendationService.selectCandidate(
                generated.id(),
                firstStep.step(),
                new CandidateSelectRequest(firstReplacement.productId()),
                fixture.sessionToken()
        );

        RecommendationStepResponse changedStep = stepOf(changed, firstStep.step());
        assertThat(changedStep.selected().candidateId()).isEqualTo(firstReplacement.candidateId());
        assertOnlySelectedCandidateMarked(changedStep, firstReplacement.candidateId());
        assertThat(candidateRepository.findById(firstReplacement.candidateId()).orElseThrow().isUserSelected()).isTrue();
        assertThat(candidateRepository.findById(initialSelected.candidateId()).orElseThrow().isUserSelected()).isFalse();

        RecommendationResponse changedAgain = recommendationService.selectCandidate(
                generated.id(),
                firstStep.step(),
                new CandidateSelectRequest(initialSelected.productId()),
                fixture.sessionToken()
        );

        RecommendationStepResponse changedAgainStep = stepOf(changedAgain, firstStep.step());
        assertThat(changedAgainStep.selected().candidateId()).isEqualTo(initialSelected.candidateId());
        assertOnlySelectedCandidateMarked(changedAgainStep, initialSelected.candidateId());
        assertThat(candidateRepository.findById(initialSelected.candidateId()).orElseThrow().isUserSelected()).isTrue();
        assertThat(candidateRepository.findById(firstReplacement.candidateId()).orElseThrow().isUserSelected()).isFalse();
    }

    private Fixture completedQuickSurvey() {
        String sessionToken = UUID.randomUUID().toString();
        sessionRepository.save(Session.create(sessionToken));
        Long surveyId = surveyService.create(sessionToken).id();

        surveyService.saveAnswer(
                surveyId,
                QuestionCode.CONCERN,
                new AnswerSaveRequest(List.of("MOISTURE")),
                sessionToken
        );
        surveyService.saveAnswer(
                surveyId,
                QuestionCode.SKIN_TYPE,
                new AnswerSaveRequest(List.of("DRY")),
                sessionToken
        );
        surveyService.setDiagnosisMode(surveyId, new DiagnosisModeRequest("QUICK"), sessionToken);
        surveyService.complete(surveyId, sessionToken);

        return new Fixture(surveyId, sessionToken);
    }

    private RecommendationStepResponse stepOf(RecommendationResponse response, int step) {
        return response.steps().stream()
                .filter(candidateStep -> candidateStep.step() == step)
                .findFirst()
                .orElseThrow();
    }

    private void assertOnlySelectedCandidateMarked(RecommendationStepResponse step, Long selectedCandidateId) {
        Stream.concat(Stream.of(step.selected()), step.others().stream())
                .forEach(candidate -> assertThat(candidate.userSelected())
                        .isEqualTo(candidate.candidateId().equals(selectedCandidateId)));
    }

    private record Fixture(Long surveyId, String sessionToken) {
    }
}
