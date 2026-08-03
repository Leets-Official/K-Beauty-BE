package com.leets.k_beauty.domain.share.service;

import com.leets.k_beauty.domain.recommendation.dto.RecommendationResponse;
import com.leets.k_beauty.domain.recommendation.entity.Recommendation;
import com.leets.k_beauty.domain.recommendation.enums.RecommendationStatus;
import com.leets.k_beauty.domain.recommendation.repository.RecommendationRepository;
import com.leets.k_beauty.domain.recommendation.service.RecommendationService;
import com.leets.k_beauty.domain.session.entity.Session;
import com.leets.k_beauty.domain.session.repository.SessionRepository;
import com.leets.k_beauty.domain.share.dto.ShareCreateResponse;
import com.leets.k_beauty.domain.share.entity.Share;
import com.leets.k_beauty.domain.share.repository.ShareRepository;
import com.leets.k_beauty.global.exception.BusinessException;
import com.leets.k_beauty.global.exception.ErrorCode;
import com.leets.k_beauty.global.exception.ShareNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareService {

    private final ShareRepository shareRepository;
    private final SessionRepository sessionRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationService recommendationService;

    @Transactional
    public ShareCreateResponse create(String sessionToken) {
        Session session = sessionRepository.findBySessionTokenHash(Session.hashToken(sessionToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        Recommendation recommendation = recommendationRepository
                .findFirstBySessionIdAndStatusOrderByIdDesc(session.getId(), RecommendationStatus.GENERATED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        Share share = shareRepository.findByRecommendationId(recommendation.getId())
                .orElseGet(() -> shareRepository.save(Share.create(recommendation.getId())));

        return ShareCreateResponse.from(share);
    }

    public RecommendationResponse getByToken(String shareToken) {
        Share share = shareRepository.findByShareToken(shareToken)
                .orElseThrow(ShareNotFoundException::new);

        return recommendationService.getByIdWithoutAuth(share.getRecommendationId());
    }
}
