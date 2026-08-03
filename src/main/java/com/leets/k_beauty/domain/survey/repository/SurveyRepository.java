package com.leets.k_beauty.domain.survey.repository;

import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    @Query("select s from Survey s where s.questionCode = :code and s.active = true")
    Optional<Survey> findActive(@Param("code") QuestionCode code);

    @Query("select s from Survey s where s.active = true order by s.surveyStep asc, s.displayOrder asc")
    List<Survey> findAllOrdered();

    @Query("select s from Survey s where s.active = true and s.surveyStep > :step order by s.surveyStep asc, s.displayOrder asc")
    List<Survey> findAfterOrdered(@Param("step") Integer step);

    // 설문 흐름상 맨 처음 질문 (신규 시작 / 재시작 시 사용)
    default Optional<Survey> findFirst() {
        return findAllOrdered().stream().findFirst();
    }

    // 특정 질문 다음에 오는 기본 순서상의 질문 (선택지별 분기가 없을 때 사용)
    default Optional<Survey> findNext(Integer step) {
        return findAfterOrdered(step).stream().findFirst();
    }
}
