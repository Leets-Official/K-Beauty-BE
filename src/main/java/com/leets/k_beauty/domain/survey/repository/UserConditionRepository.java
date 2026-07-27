package com.leets.k_beauty.domain.survey.repository;

import com.leets.k_beauty.domain.survey.entity.UserCondition;
import com.leets.k_beauty.domain.survey.enums.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {

    @Query("select c from UserCondition c where c.sessionId = :sessionId and c.status = :status")
    Optional<UserCondition> findBySession(@Param("sessionId") Long sessionId, @Param("status") SurveyStatus status);
}
