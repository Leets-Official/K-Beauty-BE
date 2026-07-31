package com.leets.k_beauty.domain.survey.entity;

import com.leets.k_beauty.domain.survey.enums.DiagnosisMode;
import com.leets.k_beauty.domain.survey.enums.SensitivityStatus;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_conditions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DiagnosisMode diagnosisMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SensitivityStatus sensitivityStatus;

    @Column(nullable = false)
    private boolean typeNeutralMode;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    public static UserCondition start(Long sessionId) {
        UserCondition userCondition = new UserCondition();
        userCondition.sessionId = sessionId;
        userCondition.status = SurveyStatus.IN_PROGRESS;
        userCondition.sensitivityStatus = SensitivityStatus.UNASSESSED;
        return userCondition;
    }

    public void updateDiagnosisMode(DiagnosisMode diagnosisMode) {
        this.diagnosisMode = diagnosisMode;
    }

    public void updateSensitivityStatus(SensitivityStatus sensitivityStatus) {
        this.sensitivityStatus = sensitivityStatus;
    }

    public void updateTypeNeutralMode(boolean typeNeutralMode) {
        this.typeNeutralMode = typeNeutralMode;
    }

    public void complete() {
        this.status = SurveyStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void abandon() {
        this.status = SurveyStatus.ABANDONED;
    }

    public boolean isInProgress() {
        return this.status == SurveyStatus.IN_PROGRESS;
    }
}
