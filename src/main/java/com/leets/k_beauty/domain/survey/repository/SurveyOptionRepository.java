package com.leets.k_beauty.domain.survey.repository;

import com.leets.k_beauty.domain.survey.entity.Survey;
import com.leets.k_beauty.domain.survey.entity.SurveyOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {

    @Query("select o from SurveyOption o where o.survey = :survey and o.active = true order by o.displayOrder asc")
    List<SurveyOption> findOptions(@Param("survey") Survey survey);

    @Query("select o from SurveyOption o where o.survey = :survey and o.optionCode in :codes and o.active = true")
    List<SurveyOption> findByCodes(@Param("survey") Survey survey, @Param("codes") List<String> codes);
}
