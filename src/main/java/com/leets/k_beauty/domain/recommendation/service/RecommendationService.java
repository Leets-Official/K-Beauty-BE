package com.leets.k_beauty.domain.recommendation.service;

import com.leets.k_beauty.domain.product.dto.IngredientInfo;
import com.leets.k_beauty.domain.product.dto.ProductCandidate;
import com.leets.k_beauty.domain.recommendation.dto.ScoredCandidate;
import com.leets.k_beauty.domain.product.service.ProductQueryService;
import com.leets.k_beauty.domain.recommendation.dto.CandidateSelectRequest;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationCandidateResponse;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationResponse;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationStepResponse;
import com.leets.k_beauty.domain.recommendation.entity.Recommendation;
import com.leets.k_beauty.domain.recommendation.entity.RecommendationCandidate;
import com.leets.k_beauty.domain.recommendation.entity.RecommendationStep;
import com.leets.k_beauty.domain.recommendation.enums.RecommendationStatus;
import com.leets.k_beauty.domain.recommendation.enums.StepRole;
import com.leets.k_beauty.domain.recommendation.repository.RecommendationCandidateRepository;
import com.leets.k_beauty.domain.recommendation.repository.RecommendationRepository;
import com.leets.k_beauty.domain.recommendation.repository.RecommendationStepRepository;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;
import com.leets.k_beauty.domain.survey.repository.UserConditionRepository;
import com.leets.k_beauty.global.exception.BusinessException;
import com.leets.k_beauty.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationStepRepository stepRepository;
    private final RecommendationCandidateRepository candidateRepository;
    private final SessionRepository sessionRepository;
    private final UserConditionRepository userConditionRepository;
    private final ProductQueryService productQueryService;
    private final RecommendationScoringService scoringService;
    private final EntityManager entityManager;

    // POST /api/recommendations - 추천 결과 생성
    @Transactional
    public RecommendationResponse generate(String sessionToken) {
        Session session = resolveSession(sessionToken);

        UserCondition userCondition = userConditionRepository.findFirstBySessionIdOrderByIdDesc(session.getId())
                .filter(uc -> uc.getStatus() == SurveyStatus.COMPLETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SURVEY_NOT_COMPLETED));

        // 기존 유효 추천 무효화
        recommendationRepository.findAllBySessionIdAndStatus(session.getId(), RecommendationStatus.GENERATED)
                .forEach(Recommendation::invalidate);

        // 새 추천 생성
        Recommendation recommendation = Recommendation.create(
                session.getId(), userCondition.getId());
        recommendationRepository.save(recommendation);

        // 3단계 후보 생성
        buildSteps(recommendation, session);

        // 세션에 최신 추천 연결 및 완료 처리
        session.linkRecommendation(recommendation.getId());
        session.markAsCompleted();

        // 1차 캐시를 flush 후 clear해서 steps/candidates가 DB에서 새로 로딩되도록 함
        entityManager.flush();
        entityManager.clear();
        Recommendation saved = recommendationRepository.findByIdWithSteps(recommendation.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        return toResponse(saved);
    }

    // GET /api/recommendations/{recommendationId} - 추천 결과 조회
    public RecommendationResponse getById(Long recommendationId, String sessionToken) {
        Session session = resolveSession(sessionToken);
        Recommendation recommendation = findOwnedRecommendation(recommendationId, session.getId());
        return toResponse(recommendation);
    }

    // GET /api/recommendations/current - 현재 세션 최신 추천 조회
    public RecommendationResponse getCurrent(String sessionToken) {
        Session session = resolveSession(sessionToken);
        Recommendation latest = recommendationRepository
                .findFirstBySessionIdAndStatusOrderByIdDesc(session.getId(), RecommendationStatus.GENERATED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        Recommendation recommendation = recommendationRepository.findByIdWithSteps(latest.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        return toResponse(recommendation);
    }

    // 공유 링크에서 인증 없이 추천 결과 조회 (ShareService 전용)
    public RecommendationResponse getByIdWithoutAuth(Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findByIdWithSteps(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        return toResponse(recommendation);
    }

    // PATCH /api/recommendations/{recommendationId}/steps/{step}/select - 후보 상품 교체
    @Transactional
    public RecommendationResponse selectCandidate(Long recommendationId, int step,
                                                  CandidateSelectRequest request, String sessionToken) {
        Session session = resolveSession(sessionToken);
        Recommendation recommendation = findOwnedRecommendation(recommendationId, session.getId());

        RecommendationStep recommendationStep = stepRepository
                .findByRecommendationAndStep(recommendation, step)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_STEP_NOT_FOUND));

        RecommendationCandidate candidate = candidateRepository
                .findByStepAndProductId(recommendationStep, request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_IN_STEP));

        recommendationStep.selectCandidate(candidate.getId());

        return toResponse(recommendation);
    }

    // ---- private helpers ----

    private void buildSteps(Recommendation recommendation, Session session) {
        StepRole[] roles = {StepRole.TEXTURE, StepRole.INTENSIVE, StepRole.MOISTURE};

        for (int stepNum = 1; stepNum <= 3; stepNum++) {
            RecommendationStep step = RecommendationStep.of(recommendation, stepNum, roles[stepNum - 1]);
            stepRepository.save(step);

            List<ScoredCandidate> candidates = scoringService.getCandidatesForStep(stepNum, session);
            for (int rank = 1; rank <= candidates.size(); rank++) {
                ScoredCandidate scored = candidates.get(rank - 1);
                RecommendationCandidate candidate = RecommendationCandidate.of(
                        step, scored.product().productId(), rank);
                candidate.updateScoreAndReason(scored.matchScore(), scored.reasonShort());
                candidateRepository.save(candidate);

                // rank 1이 기본 선택
                if (rank == 1) {
                    step.selectCandidate(candidate.getId());
                }
            }
        }
    }

    private Session resolveSession(String sessionToken) {
        return sessionRepository.findBySessionTokenHash(Session.hashToken(sessionToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private Recommendation findOwnedRecommendation(Long recommendationId, Long sessionId) {
        Recommendation recommendation = recommendationRepository.findByIdWithSteps(recommendationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        if (!recommendation.getSessionId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_FORBIDDEN);
        }
        return recommendation;
    }

    private RecommendationResponse toResponse(Recommendation recommendation) {
        List<Long> productIds = recommendation.getSteps().stream()
                .flatMap(s -> s.getCandidates().stream())
                .map(RecommendationCandidate::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductCandidate> productMap = buildProductMap(productIds);

        List<RecommendationStepResponse> stepResponses = recommendation.getSteps().stream()
                .map(step -> toStepResponse(step, productMap))
                .toList();

        return RecommendationResponse.of(recommendation, stepResponses);
    }

    private Map<Long, ProductCandidate> buildProductMap(List<Long> productIds) {
        return productQueryService.findCandidatesByIds(productIds).stream()
                .collect(Collectors.toMap(
                        ProductCandidate::productId,
                        Function.identity()
                ));
    }

    private RecommendationStepResponse toStepResponse(RecommendationStep step, Map<Long, ProductCandidate> productMap) {
        // 활성 상태인 후보만 필터링 (candidates는 candidateRank ASC 정렬 상태)
        List<RecommendationCandidate> activeCandidates = step.getCandidates().stream()
                .filter(c -> productMap.containsKey(c.getProductId()))
                .toList();

        if (activeCandidates.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_ACTIVE_PRODUCT_FOR_STEP);
        }

        // 원래 선택된 후보가 여전히 활성이면 유지, 비활성이면 rank 가장 낮은 활성 후보로 대체
        boolean originalIsActive = activeCandidates.stream()
                .anyMatch(c -> c.getId().equals(step.getSelectedCandidateId()));
        Long effectiveSelectedId = originalIsActive
                ? step.getSelectedCandidateId()
                : activeCandidates.get(0).getId();

        RecommendationCandidateResponse selected = null;
        List<RecommendationCandidateResponse> others = new ArrayList<>();

        for (RecommendationCandidate candidate : activeCandidates) {
            ProductCandidate product = productMap.get(candidate.getProductId());
            RecommendationCandidateResponse response = RecommendationCandidateResponse.of(candidate, product);

            if (candidate.getId().equals(effectiveSelectedId)) {
                selected = response;
            } else {
                others.add(response);
            }
        }

        return new RecommendationStepResponse(step.getId(), step.getStep(), step.getRole(), selected, others);
    }
}
