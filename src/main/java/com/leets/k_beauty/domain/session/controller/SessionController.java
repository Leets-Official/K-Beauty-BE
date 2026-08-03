package com.leets.k_beauty.domain.session.controller;

import com.leets.k_beauty.domain.session.dto.SessionCreateResponse;
import com.leets.k_beauty.domain.session.dto.SessionCurrentResponse;
import com.leets.k_beauty.domain.session.service.SessionService;
import com.leets.k_beauty.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Session", description = "세션 관리 API")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "세션 생성", description = "새로운 익명 세션을 생성하고 세션 토큰을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "세션 생성 성공")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SessionCreateResponse>> createSession() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(sessionService.createSession()));
    }

    @Operation(summary = "현재 세션 조회", description = "세션 토큰으로 현재 세션 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "X-Session-Token 헤더 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 없음")
    })
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SessionCurrentResponse>> getCurrentSession(
            @Parameter(description = "세션 토큰", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getCurrentSession(sessionToken)));
    }

    @Operation(summary = "세션 재시작", description = "기존 세션을 RESTARTED 상태로 변경하고 새 세션을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "X-Session-Token 헤더 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 없음")
    })
    @PostMapping("/restart")
    public ResponseEntity<ApiResponse<SessionCreateResponse>> restartSession(
            @Parameter(description = "세션 토큰", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.restartSession(sessionToken)));
    }
}
