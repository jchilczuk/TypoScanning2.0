package pl.jchilczuk.typoscanningapi.application.heuristic;

import pl.jchilczuk.typoscanningapi.domain.valueobject.DetectedSignal;
import pl.jchilczuk.typoscanningapi.domain.valueobject.RiskAssessment;

import java.util.List;

public interface RiskScorer {
    RiskAssessment score(List<DetectedSignal> signals);
}