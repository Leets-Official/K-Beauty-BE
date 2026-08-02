package com.leets.k_beauty.domain.recommendation.entity;

import com.leets.k_beauty.domain.recommendation.enums.RecommendationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private Long userConditionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;

    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("step ASC")
    private List<RecommendationStep> steps = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static Recommendation create(Long sessionId, Long userConditionId) {
        Recommendation recommendation = new Recommendation();
        recommendation.sessionId = sessionId;
        recommendation.userConditionId = userConditionId;
        recommendation.status = RecommendationStatus.GENERATED;
        return recommendation;
    }

    public void invalidate() {
        this.status = RecommendationStatus.INVALIDATED;
    }
}
