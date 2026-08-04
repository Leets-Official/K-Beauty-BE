package com.leets.k_beauty.domain.survey.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnswerSaveRequest(
        @NotEmpty(message = "선택지를 최소 1개 선택해야 합니다.")
        List<String> optionCodes
) {
}
