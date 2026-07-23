package com.leets.k_beauty.domain.session.enums;

public enum SensitivityStatus {
    UNASSESSED, // 빠른 진단으로 민감도 미응답
    LOW,        // Survey-1에서 아니요 선택
    MEDIUM,     // 예 + 잘 모르겠어요 or 주의 카테고리 1개
    HIGH        // 예 + 주의 카테고리 2개 이상
}
