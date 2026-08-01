package com.leets.k_beauty.domain.recommendation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private RecommendationStep step;

    @Column(nullable = false)
    private Long productId;

    // 1=메인 추천, 2~3=다른 후보
    @Column(name = "`rank`", nullable = false)
    private int rank;

    public static RecommendationCandidate of(RecommendationStep step, Long productId, int rank) {
        RecommendationCandidate candidate = new RecommendationCandidate();
        candidate.step = step;
        candidate.productId = productId;
        candidate.rank = rank;
        return candidate;
    }
}
