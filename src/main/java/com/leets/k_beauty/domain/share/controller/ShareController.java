package com.leets.k_beauty.domain.share.controller;

import com.leets.k_beauty.domain.recommendation.dto.RecommendationResponse;
import com.leets.k_beauty.domain.share.dto.ShareCreateResponse;
import com.leets.k_beauty.domain.share.service.ShareService;
import com.leets.k_beauty.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Share", description = "공유 링크 API")
@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @Operation(summary = "공유 링크 생성", description = "현재 세션의 최신 추천 결과를 공유하는 링크를 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "공유 링크 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "X-Session-Token 헤더 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션 또는 추천 결과 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ShareCreateResponse>> create(
            @Parameter(description = "세션 토큰", required = true)
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(shareService.create(sessionToken)));
    }

    @Operation(summary = "공유 링크로 추천 결과 조회", description = "shareToken으로 추천 결과를 조회합니다. 세션 인증 없이 접근 가능합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공유 링크 없음")
    })
    @GetMapping("/{shareToken}")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getByToken(
            @PathVariable String shareToken) {
        return ResponseEntity.ok(ApiResponse.success(shareService.getByToken(shareToken)));
    }
}
