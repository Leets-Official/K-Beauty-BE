package com.leets.k_beauty.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SESSION_NOT_FOUND(404, "세션을 찾을 수 없습니다."),
    MISSING_SESSION_TOKEN(400, "X-Session-Token 헤더가 필요합니다."),
    SESSION_NOT_IN_PROGRESS(400, "진행 중인 세션만 재시작할 수 있습니다."),

    // ---- Survey: 조회/소유권 ----
    SURVEY_NOT_FOUND(404, "설문 진행 정보를 찾을 수 없습니다."),
    SURVEY_FORBIDDEN(403, "해당 설문에 접근할 권한이 없습니다."),
    SURVEY_QUESTION_NOT_FOUND(404, "설문 질문 정보를 찾을 수 없습니다."),
    NO_PREVIOUS_QUESTION(404, "이전 질문이 없습니다."),

    // ---- Survey: 생성/완료/재시작 상태 ----
    SURVEY_IN_PROGRESS_EXISTS(409, "진행 중이거나 완료된 설문이 이미 존재합니다."),
    SURVEY_ALREADY_COMPLETED(409, "이미 완료된 설문입니다."),
    SURVEY_ANSWER_MISSING(422, "설문 완료에 필요한 답변이 누락되었습니다."),

    // ---- Survey: 답변 저장 검증 ----
    SINGLE_SELECTION_VIOLATION(400, "단일 선택 질문에는 선택지 하나만 저장할 수 있습니다."),
    MAX_SELECTION_EXCEEDED(400, "선택 가능한 개수를 벗어났습니다."),
    SURVEY_OPTION_INVALID(400, "질문에 속하지 않거나 사용할 수 없는 선택지입니다."),
    EXCLUSIVE_OPTION_VIOLATION(422, "배타 선택지는 다른 항목과 함께 선택할 수 없습니다."),
    CAUTION_PREREQUISITE_MISSING(409, "민감 여부에 '예'로 응답한 경우에만 주의 요소를 저장할 수 있습니다."),

    // ---- Survey: 진단 경로 ----
    INVALID_DIAGNOSIS_MODE(400, "유효하지 않은 진단 경로입니다."),
    DIAGNOSIS_MODE_PREREQUISITE_MISSING(409, "핵심 고민과 피부 타입을 먼저 선택해야 합니다."),

    // ---- Recommendation ----
    SURVEY_NOT_COMPLETED(409, "설문이 완료되지 않아 추천을 생성할 수 없습니다."),
    RECOMMENDATION_NOT_FOUND(404, "추천 결과를 찾을 수 없습니다."),
    RECOMMENDATION_FORBIDDEN(403, "해당 추천 결과에 접근할 권한이 없습니다."),
    RECOMMENDATION_STEP_NOT_FOUND(404, "해당 단계의 추천 정보를 찾을 수 없습니다."),
    CANDIDATE_NOT_IN_STEP(400, "해당 단계의 후보 목록에 없는 상품입니다."),
    NO_ACTIVE_PRODUCT_FOR_STEP(503, "해당 단계에 활성화된 후보 상품이 없어 추천을 표시할 수 없습니다."),

    // ---- Product ----
    PRODUCT_NOT_FOUND(404, "상품 정보를 찾을 수 없습니다."),

    // ---- Share ----
    SHARE_NOT_FOUND(404, "공유 링크를 찾을 수 없습니다.");

    private final int status;
    private final String message;
}
