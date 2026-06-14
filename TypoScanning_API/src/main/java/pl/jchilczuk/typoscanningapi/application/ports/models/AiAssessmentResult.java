package pl.jchilczuk.typoscanningapi.application.ports.models;

public record AiAssessmentResult(
        String classification,
        String suspicionLevel,
        String confidence,
        String recommendedAction,
        String explanation
) {}