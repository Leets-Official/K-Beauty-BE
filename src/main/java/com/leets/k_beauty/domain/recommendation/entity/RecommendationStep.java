package com.leets.k_beauty.domain.recommendation.entity;

import com.leets.k_beauty.domain.recommendation.enums.StepRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendation_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    // 1=피부결 정돈, 2=집중 케어, 3=보습 마무리
    @Column(nullable = false)
    private int step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepRole role;

    // rank 1 후보가 기본 선택, 사용자가 교체하면 변경됨
    private Long selectedCandidateId;

    @OneToMany(mappedBy = "step", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("rank ASC")
    private List<RecommendationCandidate> candidates = new ArrayList<>();

    public static RecommendationStep of(Recommendation recommendation, int step, StepRole role) {
        RecommendationStep s = new RecommendationStep();
        s.recommendation = recommendation;
        s.step = step;
        s.role = role;
        return s;
    }

    public void selectCandidate(Long candidateId) {
        this.selectedCandidateId = candidateId;
    }
}
