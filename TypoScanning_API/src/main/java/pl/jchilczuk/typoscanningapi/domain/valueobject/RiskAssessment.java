package pl.jchilczuk.typoscanningapi.domain.valueobject;

import pl.jchilczuk.typoscanningapi.domain.enums.RiskLevel;

import java.util.List;

public record RiskAssessment(
        int score,
        RiskLevel riskLevel,
        List<DetectedSignal> detectedSignals
) {
}