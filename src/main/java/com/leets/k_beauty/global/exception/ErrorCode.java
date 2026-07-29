package com.leets.k_beauty.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SESSION_NOT_FOUND(404, "세션을 찾을 수 없습니다."),
    MISSING_SESSION_TOKEN(400, "X-Session-Token 헤더가 필요합니다.");

    private final int status;
    private final String message;
}
