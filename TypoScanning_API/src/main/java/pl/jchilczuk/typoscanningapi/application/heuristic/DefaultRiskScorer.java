package pl.jchilczuk.typoscanningapi.application.heuristic;

import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.domain.enums.RiskLevel;
import pl.jchilczuk.typoscanningapi.domain.valueobject.DetectedSignal;
import pl.jchilczuk.typoscanningapi.domain.valueobject.RiskAssessment;

import java.util.List;

@Service
public class DefaultRiskScorer implements RiskScorer {

    @Override
    public RiskAssessment score(List<DetectedSignal> signals) {
        int totalScore = signals.stream()
                .mapToInt(DetectedSignal::scoreContribution)
                .sum();

        RiskLevel riskLevel;
        if (totalScore >= 6) {
            riskLevel = RiskLevel.HIGH;
        } else if (totalScore >= 3) {
            riskLevel = RiskLevel.MEDIUM;
        } else {
            riskLevel = RiskLevel.LOW;
        }

        return new RiskAssessment(totalScore, riskLevel, signals);
    }
}