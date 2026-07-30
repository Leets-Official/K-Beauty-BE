package com.leets.k_beauty.global.exception;

import com.leets.k_beauty.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getStatus(), errorCode.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        if ("X-Session-Token".equals(e.getHeaderName())) {
            ErrorCode errorCode = ErrorCode.MISSING_SESSION_TOKEN;
            return ResponseEntity.status(errorCode.getStatus())
                    .body(ApiResponse.error(errorCode.getStatus(), errorCode.getMessage()));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다."));
    }
}
