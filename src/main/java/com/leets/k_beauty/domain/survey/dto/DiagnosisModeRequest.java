package com.leets.k_beauty.domain.survey.dto;

import jakarta.validation.constraints.NotBlank;

public record DiagnosisModeRequest(
        @NotBlank
        String diagnosisMode
) {
}
