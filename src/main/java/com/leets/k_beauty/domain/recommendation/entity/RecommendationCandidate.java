package com.leets.k_beauty.domain.recommendation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "recommendation_candidates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"step_id", "candidate_rank"})
)
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
    @Column(name = "candidate_rank", nullable = false)
    private int candidateRank;

    @Column(nullable = false)
    private boolean isUserSelected = false;

    @Column(precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Column(length = 200)
    private String reasonShort;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static RecommendationCandidate of(RecommendationStep step, Long productId, int candidateRank) {
        RecommendationCandidate candidate = new RecommendationCandidate();
        candidate.step = step;
        candidate.productId = productId;
        candidate.candidateRank = candidateRank;
        candidate.isUserSelected = false;
        return candidate;
    }

    public void markAsUserSelected() {
        this.isUserSelected = true;
    }

    public void clearUserSelected() {
        this.isUserSelected = false;
    }
}
