package pl.jchilczuk.typoscanningapi.api.dto;

public record DetectedSignalResponse(
        String signalType,
        String description,
        int scoreContribution
) {
}