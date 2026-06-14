package pl.jchilczuk.typoscanningapi.application.heuristic;

import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;
import pl.jchilczuk.typoscanningapi.domain.valueobject.RiskAssessment;

public interface HeuristicAssessmentService {
    RiskAssessment assess(String sourceDomain, DomainVariantResult result);
}