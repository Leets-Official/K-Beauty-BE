package com.leets.k_beauty.domain.survey.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnswerSaveRequest(
        @NotEmpty
        List<String> optionCodes
) {
}
