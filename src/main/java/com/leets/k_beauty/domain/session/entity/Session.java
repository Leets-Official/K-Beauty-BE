package com.leets.k_beauty.domain.session.entity;

import com.leets.k_beauty.domain.session.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(nullable = false, unique = true, length = 36)
    private String sessionToken;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SearchHabit searchHabit;

    private Long recommendationId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static Session create() {
        Session session = new Session();
        session.sessionToken = UUID.randomUUID().toString();
        session.status = SessionStatus.IN_PROGRESS;
        session.sensitivityStatus = SensitivityStatus.UNASSESSED;
        session.typeNeutralMode = false;
        session.cautionCategories = new ArrayList<>();
        return session;
    }

    public void markAsRestarted() {
        this.status = SessionStatus.RESTARTED;
    }

    public void linkRecommendation(Long recommendationId) {
        this.recommendationId = recommendationId;
    }
}
