package com.leets.k_beauty.domain.recommendation.controller;

import com.leets.k_beauty.domain.recommendation.dto.CandidateSelectRequest;
import com.leets.k_beauty.domain.recommendation.dto.RecommendationResponse;
import com.leets.k_beauty.domain.recommendation.service.RecommendationService;
import com.leets.k_beauty.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recommendation", description = "추천 결과 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "추천 결과 생성",
            description = "완료된 설문을 기반으로 3단계 루틴 추천 결과를 생성합니다. " +
                    "이전에 생성된 유효한 추천이 있으면 자동으로 무효화됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> generate(
            @Parameter(description = "세션 토큰", required = true)
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(recommendationService.generate(sessionToken)));
    }

    @Operation(summary = "현재 세션 최신 추천 조회",
            description = "현재 세션의 가장 최신 유효 추천 결과를 조회합니다.")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getCurrent(
            @Parameter(description = "세션 토큰", required = true)
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getCurrent(sessionToken)));
    }

    @Operation(summary = "추천 결과 조회",
            description = "추천 ID로 특정 추천 결과를 조회합니다.")
    @GetMapping("/{recommendationId}")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getById(
            @PathVariable Long recommendationId,
            @Parameter(description = "세션 토큰", required = true)
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(recommendationService.getById(recommendationId, sessionToken)));
    }

    @Operation(summary = "후보 상품 교체",
            description = "특정 단계의 추천 제품을 같은 단계 후보 중 하나로 교체합니다.")
    @PatchMapping("/{recommendationId}/steps/{step}/select")
    public ResponseEntity<ApiResponse<RecommendationResponse>> selectCandidate(
            @PathVariable Long recommendationId,
            @PathVariable int step,
            @Valid @RequestBody CandidateSelectRequest request,
            @Parameter(description = "세션 토큰", required = true)
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.selectCandidate(recommendationId, step, request, sessionToken)));
    }
}
