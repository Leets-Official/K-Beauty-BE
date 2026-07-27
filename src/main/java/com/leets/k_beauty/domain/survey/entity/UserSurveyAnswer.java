package com.leets.k_beauty.domain.survey.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_survey_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSurveyAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_condition_id", nullable = false)
    private UserCondition userCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private SurveyOption option;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static UserSurveyAnswer of(UserCondition userCondition, SurveyOption option) {
        UserSurveyAnswer answer = new UserSurveyAnswer();
        answer.userCondition = userCondition;
        answer.option = option;
        return answer;
    }
}
