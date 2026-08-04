package com.leets.k_beauty.global.exception;

import com.leets.k_beauty.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * <p>Exception 하나만 잡으면 Spring이 400으로 바꿔주던 예외까지 전부 500이 된다.
 * Spring은 @ExceptionHandler를 먼저 찾고, 거기서 잡히면 기본 변환 단계를 건너뛰기 때문
 * <p>로깅 기준: 규칙에 따라 거절한 요청은 warn, 예상하지 못한 오류는 스택 트레이스와 함께 error..
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e,
                                                                     HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        // 어떤 요청이 왜 막혔는지만 남김
        log.warn("[{}] {} - {}", errorCode.name(), describe(request), errorCode.getMessage());
        return toResponse(errorCode);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e,
                                                                 HttpServletRequest request) {
        if (SESSION_TOKEN_HEADER.equals(e.getHeaderName())) {
            log.warn("세션 토큰 헤더 누락 - {}", describe(request));
            return toResponse(ErrorCode.MISSING_SESSION_TOKEN);
        }

        log.warn("필수 헤더 누락 - {} - {}", describe(request), e.getHeaderName());
        return toResponse(ErrorCode.INVALID_REQUEST, e.getHeaderName() + " 헤더가 필요합니다.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e,
                                                              HttpServletRequest request) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (detail.isBlank()) {
            detail = ErrorCode.INVALID_REQUEST.getMessage();
        }

        log.warn("요청 검증 실패 - {} - {}", describe(request), detail);
        return toResponse(ErrorCode.INVALID_REQUEST, detail);
    }

    /**
     * 경로 변수나 쿼리 파라미터의 타입이 맞지 않는 경우.
     * 없는 질문 코드({@code /answers/NOT_EXIST})나 숫자가 아닌 ID가 여기로 온다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                                HttpServletRequest request) {
        String detail = e.getName() + ": '" + e.getValue() + "' 값은 사용할 수 없습니다.";

        log.warn("요청 값 바인딩 실패 - {} - {}", describe(request), detail);
        return toResponse(ErrorCode.INVALID_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e,
                                                               HttpServletRequest request) {
        // 원본 메시지에는 내부 클래스명과 패키지 경로가 들어 있다. 응답에 실으면 서버 구조가 새어 나가므로
        // 자세한 내용은 로그에만 남긴다.
        log.warn("요청 본문 파싱 실패 - {}", describe(request), e);
        return toResponse(ErrorCode.MALFORMED_REQUEST_BODY);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                      HttpServletRequest request) {
        log.warn("지원하지 않는 메서드 - {}", describe(request));
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e,
                                                              HttpServletRequest request) {
        log.warn("존재하지 않는 경로 - {}", describe(request));
        return toResponse(ErrorCode.ENDPOINT_NOT_FOUND);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        // 스택 트레이스가 없으면 원인을 찾을 방법이 없다. 마지막 인자로 넘겨야 전체가 출력된다.
        log.error("처리되지 않은 예외 - {}", describe(request), e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode) {
        return toResponse(errorCode, errorCode.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getStatus(), message));
    }

    private String describe(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }
}