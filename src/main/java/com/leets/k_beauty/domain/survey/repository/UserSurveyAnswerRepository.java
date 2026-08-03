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

    // 답변을 다시 저장하면 기존 행을 지우고 새로 넣기 때문에 createdAt이 갱신됨.
    // 여러 개를 한 번에 저장할 때 createdAt이 서로 같아 순서가 보장되지 않으므로 id를 기준으로 정렬
    @Query("select a from UserSurveyAnswer a where a.userCondition = :uc order by a.id asc")
    List<UserSurveyAnswer> findAllByCondition(@Param("uc") UserCondition userCondition);

    @Query("select a from UserSurveyAnswer a where a.userCondition = :uc and a.option.survey = :survey")
    List<UserSurveyAnswer> findByConditionAndSurvey(@Param("uc") UserCondition userCondition, @Param("survey") Survey survey);

    @Modifying
    @Query("delete from UserSurveyAnswer a where a.userCondition = :uc and a.option.survey = :survey")
    void deleteByConditionAndSurvey(@Param("uc") UserCondition userCondition, @Param("survey") Survey survey);
}
