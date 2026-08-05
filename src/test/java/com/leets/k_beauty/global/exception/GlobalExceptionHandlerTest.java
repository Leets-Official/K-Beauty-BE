package com.leets.k_beauty.global.exception;

import com.leets.k_beauty.domain.survey.controller.SurveyController;
import com.leets.k_beauty.domain.survey.service.SurveyService;
import com.leets.k_beauty.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러에 닿기 전이나 진입 지점에서 발생하는 예외의 응답 검증.
 */
@DisplayName("전역 예외 처리")
@WebMvcTest(SurveyController.class)
@Import(SecurityConfig.class)
class GlobalExceptionHandlerTest {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";
    private static final String SESSION_TOKEN = "test-session-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SurveyService surveyService;

    @Nested
    @DisplayName("요청 본문 검증")
    class RequestBodyValidation {

        @Test
        @DisplayName("선택지가 빈 배열이면 400을 반환한다")
        void rejectsEmptyOptionCodes() throws Exception {
            mockMvc.perform(put("/api/surveys/1/answers/CONCERN")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionCodes\": []}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("진단 경로가 빈 문자열이면 400을 반환한다")
        void rejectsBlankDiagnosisMode() throws Exception {
            mockMvc.perform(patch("/api/surveys/1/diagnosis-mode")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"diagnosisMode\": \"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        /**
         * 검증 문구를 DTO에 직접 지정했는지 확인하는 테스트다
         */
        @Test
        @DisplayName("어떤 필드가 왜 잘못됐는지 응답 메시지에 담는다")
        void includesFieldNameAndReasonInMessage() throws Exception {
            mockMvc.perform(put("/api/surveys/1/answers/CONCERN")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionCodes\": []}"))
                    .andExpect(jsonPath("$.message").value("optionCodes: 선택지를 최소 1개 선택해야 합니다."));
        }

        /**
         * 값도 닫는 괄호도 없는 JSON이다. 파싱 단계에서 실패해야 이 경로를 탄다.
         * 검증 실패도 같은 400이라, 메시지까지 봐야 어느 쪽인지 구분된다.
         */
        @Test
        @DisplayName("본문 JSON이 깨져 있으면 400을 반환한다")
        void rejectsMalformedJson() throws Exception {
            mockMvc.perform(put("/api/surveys/1/answers/CONCERN")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionCodes\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(ErrorCode.MALFORMED_REQUEST_BODY.getMessage()));
        }
    }

    @Nested
    @DisplayName("경로 변수 바인딩")
    class PathVariableBinding {

        @Test
        @DisplayName("정의되지 않은 질문 코드면 400을 반환한다")
        void rejectsUnknownQuestionCode() throws Exception {
            mockMvc.perform(put("/api/surveys/1/answers/NOT_EXIST")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionCodes\": [\"MOISTURE\"]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("설문 ID가 숫자가 아니면 400을 반환한다")
        void rejectsNonNumericSurveyId() throws Exception {
            mockMvc.perform(get("/api/surveys/not-a-number")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("요청 자체가 잘못된 경우")
    class InvalidRequest {

        @Test
        @DisplayName("세션 토큰 헤더가 없으면 400을 반환한다")
        void rejectsMissingSessionToken() throws Exception {
            mockMvc.perform(get("/api/surveys/current"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(ErrorCode.MISSING_SESSION_TOKEN.getMessage()));
        }

        @Test
        @DisplayName("지원하지 않는 HTTP 메서드면 405를 반환한다")
        void rejectsUnsupportedMethod() throws Exception {
            mockMvc.perform(delete("/api/surveys/1")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405));
        }

        @Test
        @DisplayName("존재하지 않는 경로면 404를 반환한다")
        void rejectsUnknownPath() throws Exception {
            mockMvc.perform(get("/api/no-such-endpoint")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("도메인 예외")
    class DomainException {

        @Test
        @DisplayName("BusinessException은 ErrorCode의 상태 코드와 메시지를 그대로 내려준다")
        void mapsBusinessExceptionToErrorCode() throws Exception {
            given(surveyService.getProgress(anyLong(), anyString()))
                    .willThrow(new BusinessException(ErrorCode.SURVEY_FORBIDDEN));

            mockMvc.perform(get("/api/surveys/1")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SURVEY_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("예상하지 못한 예외는 500과 고정 문구로 내려준다")
        void hidesUnexpectedExceptionDetail() throws Exception {
            given(surveyService.getProgress(anyLong(), anyString()))
                    .willThrow(new IllegalStateException("DB 접속 정보: root/secret"));

            mockMvc.perform(get("/api/surveys/1")
                            .header(SESSION_TOKEN_HEADER, SESSION_TOKEN))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
        }
    }
}
