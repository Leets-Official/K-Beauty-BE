package com.leets.k_beauty.domain.recommendation.repository;

import com.leets.k_beauty.domain.recommendation.entity.Recommendation;
import com.leets.k_beauty.domain.recommendation.entity.RecommendationStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationStepRepository extends JpaRepository<RecommendationStep, Long> {

    Optional<RecommendationStep> findByRecommendationAndStep(Recommendation recommendation, int step);
}
