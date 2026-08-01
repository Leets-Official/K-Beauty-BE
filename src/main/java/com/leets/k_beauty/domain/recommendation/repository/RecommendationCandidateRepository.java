package com.leets.k_beauty.domain.recommendation.repository;

import com.leets.k_beauty.domain.recommendation.entity.RecommendationCandidate;
import com.leets.k_beauty.domain.recommendation.entity.RecommendationStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationCandidateRepository extends JpaRepository<RecommendationCandidate, Long> {

    Optional<RecommendationCandidate> findByStepAndProductId(RecommendationStep step, Long productId);
}
