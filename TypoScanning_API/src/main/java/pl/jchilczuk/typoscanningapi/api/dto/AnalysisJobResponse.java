package pl.jchilczuk.typoscanningapi.api.dto;

import java.time.OffsetDateTime;

public record AnalysisJobResponse(
        Long jobId,
        String sourceDomain,
        String status,
        OffsetDateTime createdAt
) {
}