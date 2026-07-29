package com.leets.k_beauty.domain.survey.repository;

import com.leets.k_beauty.domain.survey.entity.UserCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {

    // 세션의 가장 최근 설문 컨텍스트 (상태 무관)
    // - getCurrent: 재진입 라우팅(이어하기/결과화면/온보딩 분기)에 사용
    // - create: 마지막 컨텍스트가 ABANDONED가 아니면(IN_PROGRESS 또는 COMPLETED) 새 컨텍스트 생성을 막는 데 사용
    Optional<UserCondition> findFirstBySessionIdOrderByIdDesc(Long sessionId);
}
