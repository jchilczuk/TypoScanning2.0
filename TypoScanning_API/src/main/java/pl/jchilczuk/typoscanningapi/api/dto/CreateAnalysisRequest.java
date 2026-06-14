package pl.jchilczuk.typoscanningapi.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAnalysisRequest(
        @NotBlank String sourceDomain
) {
}
