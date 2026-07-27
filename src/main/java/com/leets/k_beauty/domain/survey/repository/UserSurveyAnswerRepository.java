package com.leets.k_beauty.domain.survey.repository;

import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.entity.UserSurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSurveyAnswerRepository extends JpaRepository<UserSurveyAnswer, Long> {

    @Query("select a from UserSurveyAnswer a where a.userCondition = :uc order by a.createdAt asc")
    List<UserSurveyAnswer> findAllByCondition(@Param("uc") UserCondition userCondition);

    @Query("select a from UserSurveyAnswer a where a.userCondition = :uc and a.option.survey = :survey")
    List<UserSurveyAnswer> findByConditionAndSurvey(@Param("uc") UserCondition userCondition, @Param("survey") Survey survey);

    @Modifying
    @Query("delete from UserSurveyAnswer a where a.userCondition = :uc and a.option.survey = :survey")
    void deleteByConditionAndSurvey(@Param("uc") UserCondition userCondition, @Param("survey") Survey survey);
}
