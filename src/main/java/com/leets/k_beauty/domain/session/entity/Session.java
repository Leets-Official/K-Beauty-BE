package com.leets.k_beauty.domain.session.entity;

import com.leets.k_beauty.domain.common.enums.*;
import com.leets.k_beauty.domain.session.enums.SessionStatus;
import com.leets.k_beauty.global.exception.SessionNotInProgressException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sessionTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DiagnosisType diagnosisType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SkinConcern skinConcern;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SkinType skinType;

    @Column(nullable = false)
    private boolean typeNeutralMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensitivityStatus sensitivityStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "session_caution_categories",
            joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "caution_category", length = 20)
    private List<CautionCategory> cautionCategories = new ArrayList<>();

    private Long recommendationId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    public static Session create(String rawToken) {
        Session session = new Session();
        session.sessionTokenHash = hashToken(rawToken);
        session.status = SessionStatus.IN_PROGRESS;
        session.sensitivityStatus = SensitivityStatus.UNASSESSED;
        session.typeNeutralMode = false;
        session.cautionCategories = new ArrayList<>();
        return session;
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    public void validateInProgress() {
        if (this.status != SessionStatus.IN_PROGRESS) {
            throw new SessionNotInProgressException();
        }
    }

    public void markAsCompleted() {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsRestarted() {
        this.status = SessionStatus.RESTARTED;
    }

    public void linkRecommendation(Long recommendationId) {
        this.recommendationId = recommendationId;
    }

    public void updateDiagnosisType(DiagnosisType diagnosisType) {
        this.diagnosisType = diagnosisType;
    }

    public void updateSkinConcern(SkinConcern skinConcern) {
        this.skinConcern = skinConcern;
    }

    public void updateSkinType(SkinType skinType) {
        this.skinType = skinType;
        this.typeNeutralMode = (skinType == SkinType.UNKNOWN);
    }

    public void updateSensitivityStatus(SensitivityStatus status) {
        this.sensitivityStatus = status;
    }

    public void updateCautionCategories(List<CautionCategory> categories) {
        this.cautionCategories.clear();
        this.cautionCategories.addAll(categories);
    }

    public void resetCondition() {
        this.diagnosisType = null;
        this.skinConcern = null;
        this.skinType = null;
        this.typeNeutralMode = false;
        this.sensitivityStatus = SensitivityStatus.UNASSESSED;
        this.cautionCategories.clear();
        this.recommendationId = null;
    }
}
