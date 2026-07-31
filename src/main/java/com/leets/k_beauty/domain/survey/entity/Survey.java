package com.leets.k_beauty.domain.survey.entity;

import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.enums.SelectionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "surveys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private QuestionCode questionCode;

    @Column(nullable = false, length = 200)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SelectionType selectionType;

    @Column(nullable = false)
    private Integer maxSelections;

    @Column(nullable = false)
    private Integer surveyStep;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Survey create(QuestionCode questionCode, String questionText, SelectionType selectionType,
                                 Integer maxSelections, Integer surveyStep, Integer displayOrder,
                                 boolean required, boolean active) {
        Survey survey = new Survey();
        survey.questionCode = questionCode;
        survey.questionText = questionText;
        survey.selectionType = selectionType;
        survey.maxSelections = maxSelections;
        survey.surveyStep = surveyStep;
        survey.displayOrder = displayOrder;
        survey.required = required;
        survey.active = active;
        return survey;
    }
}