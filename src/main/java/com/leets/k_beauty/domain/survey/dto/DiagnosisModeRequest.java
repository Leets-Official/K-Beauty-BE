package com.leets.k_beauty.domain.survey.dto;

import jakarta.validation.constraints.NotBlank;

public record DiagnosisModeRequest(
        @NotBlank(message = "진단 경로를 선택해야 합니다.")
        String diagnosisMode
) {
}
