package com.leets.k_beauty.domain.survey.entity;

import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "survey_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private QuestionCode nextQuestionCode;

    @Column(nullable = false, length = 50)
    private String optionCode;

    @Column(nullable = false, length = 100)
    private String optionText;

    @Column(length = 300)
    private String guideText;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(name = "is_exclusive", nullable = false)
    private boolean exclusive;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static SurveyOption create(Survey survey, String optionCode, String optionText, String guideText,
                                       Integer displayOrder, boolean exclusive, boolean active,
                                       QuestionCode nextQuestionCode) {
        SurveyOption option = new SurveyOption();
        option.survey = survey;
        option.optionCode = optionCode;
        option.optionText = optionText;
        option.guideText = guideText;
        option.displayOrder = displayOrder;
        option.exclusive = exclusive;
        option.active = active;
        option.nextQuestionCode = nextQuestionCode;
        return option;
    }
}
