package com.leets.k_beauty.domain.recommendation.repository;

import com.leets.k_beauty.domain.recommendation.entity.Recommendation;
import com.leets.k_beauty.domain.recommendation.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // steps 포함 단건 조회 (1차 캐시 우회, candidates는 트랜잭션 안에서 lazy 로딩)
    @Query("""
            SELECT DISTINCT r FROM Recommendation r
            LEFT JOIN FETCH r.steps
            WHERE r.id = :id
            """)
    Optional<Recommendation> findByIdWithSteps(@Param("id") Long id);

    // 현재 세션의 최신 유효 추천 조회 (GET /current)
    Optional<Recommendation> findFirstBySessionIdAndStatusOrderByIdDesc(Long sessionId, RecommendationStatus status);

    // 세션의 모든 유효 추천 (재생성 시 이전 것 무효화용)
    List<Recommendation> findAllBySessionIdAndStatus(Long sessionId, RecommendationStatus status);
}
