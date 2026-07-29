package com.leets.k_beauty.domain.survey.controller;

import com.leets.k_beauty.domain.survey.dto.*;
import com.leets.k_beauty.domain.survey.enums.QuestionCode;
import com.leets.k_beauty.domain.survey.service.SurveyService;
import com.leets.k_beauty.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Survey", description = "설문 진행 API")
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "새 설문 생성", description = "새로운 설문 진행 컨텍스트를 시작합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<UserConditionResponse>> create(@Parameter(description = "세션 토큰", required = true)
                                                                     @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(surveyService.create(sessionToken)));
    }

    @Operation(summary = "설문 이어하기/재진입 라우팅", description = "세션의 가장 최근 설문 컨텍스트를 상태 무관으로 조회합니다. " +
            "status가 IN_PROGRESS면 이어하기, COMPLETED면 결과 화면으로, 응답이 없으면(404) 온보딩으로 라우팅합니다.")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserConditionResponse>> getCurrent(@Parameter(description = "세션 토큰", required = true)
                                                                         @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(surveyService.getCurrent(sessionToken)));
    }

    @Operation(summary = "설문 진행 정보 조회", description = "설문 진행 정보와 저장된 답변을 조회합니다.")
    @GetMapping("/{surveyId}")
    public ResponseEntity<ApiResponse<SurveyProgressResponse>> getProgress(@PathVariable Long surveyId,
                                                                           @Parameter(description = "세션 토큰", required = true)
                                                                           @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(surveyService.getProgress(surveyId, sessionToken)));
    }

    @Operation(summary = "설문 질문 및 선택지 조회", description = "질문 코드에 해당하는 질문과 선택지를 조회합니다.")
    @GetMapping("/questions/{questionCode}")
    public ResponseEntity<ApiResponse<SurveyQuestionResponse>> getQuestion(@PathVariable QuestionCode questionCode) {
        return ResponseEntity.ok(ApiResponse.success(surveyService.getQuestion(questionCode)));
    }

    @Operation(summary = "설문 답변 저장 및 변경", description = "질문에 대한 답변을 저장하거나 변경합니다.")
    @PutMapping("/{surveyId}/answers/{questionCode}")
    public ResponseEntity<ApiResponse<AnswerSaveResponse>> saveAnswer(@PathVariable Long surveyId,
                                                                      @PathVariable QuestionCode questionCode,
                                                                      @Valid @RequestBody AnswerSaveRequest request,
                                                                      @Parameter(description = "세션 토큰", required = true)
                                                                      @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(
                surveyService.saveAnswer(surveyId, questionCode, request, sessionToken)));
    }

    @Operation(summary = "진단 경로 선택", description = "빠른 진단(QUICK) 또는 상세 진단(DETAILED) 경로를 저장합니다.")
    @PatchMapping("/{surveyId}/diagnosis-mode")
    public ResponseEntity<ApiResponse<DiagnosisModeResponse>> setDiagnosisMode(@PathVariable Long surveyId,
                                                                               @Valid @RequestBody DiagnosisModeRequest request,
                                                                               @Parameter(description = "세션 토큰", required = true)
                                                                               @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(
                surveyService.setDiagnosisMode(surveyId, request, sessionToken)));
    }

    @Operation(summary = "설문 완료", description = "필수 답변이 모두 채워졌는지 확인하고 설문을 완료 처리합니다.")
    @PostMapping("/{surveyId}/completion")
    public ResponseEntity<ApiResponse<CompletionResponse>> complete(@PathVariable Long surveyId,
                                                                    @Parameter(description = "세션 토큰", required = true)
                                                                    @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(surveyService.complete(surveyId, sessionToken)));
    }

    @Operation(summary = "이전 질문으로 돌아가기", description = "직전에 답변한 질문과 그때 선택한 답변을 프리필용으로 조회합니다.")
    @PostMapping("/{surveyId}/previous-question")
    public ResponseEntity<ApiResponse<PreviousQuestionResponse>> getPreviousQuestion(@PathVariable Long surveyId,
                                                                                     @Parameter(description = "클라이언트가 현재 보고 있는 질문 코드 (생략 시 서버가 마지막 답변 기준으로 계산)")
                                                                                     @RequestParam(required = false) QuestionCode from,
                                                                                     @Parameter(description = "세션 토큰", required = true)
                                                                                     @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.ok(ApiResponse.success(surveyService.getPreviousQuestion(surveyId, from, sessionToken)));
    }

    @Operation(summary = "설문 처음부터 다시 하기", description = "기존 설문을 종료하고 같은 세션으로 새 설문을 시작합니다.")
    @PostMapping("/{surveyId}/restart")
    public ResponseEntity<ApiResponse<RestartResponse>> restart(@PathVariable Long surveyId,
                                                                @Parameter(description = "세션 토큰", required = true)
                                                                @RequestHeader("X-Session-Token") String sessionToken) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(surveyService.restart(surveyId, sessionToken)));
    }
}
