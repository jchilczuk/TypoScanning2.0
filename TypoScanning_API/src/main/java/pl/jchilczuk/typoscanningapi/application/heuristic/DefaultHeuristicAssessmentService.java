package pl.jchilczuk.typoscanningapi.application.heuristic;

import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;
import pl.jchilczuk.typoscanningapi.domain.enums.RiskLevel;
import pl.jchilczuk.typoscanningapi.domain.enums.SignalType;
import pl.jchilczuk.typoscanningapi.domain.valueobject.DetectedSignal;
import pl.jchilczuk.typoscanningapi.domain.valueobject.RiskAssessment;

import java.util.List;

@Service
public class DefaultHeuristicAssessmentService implements HeuristicAssessmentService {

    private final SignalExtractor signalExtractor;
    private final RiskScorer riskScorer;

    public DefaultHeuristicAssessmentService(SignalExtractor signalExtractor,
                                             RiskScorer riskScorer) {
        this.signalExtractor = signalExtractor;
        this.riskScorer = riskScorer;
    }

    @Override
    public RiskAssessment assess(String sourceDomain, DomainVariantResult result) {
        if (!result.isRegistered()) {
            return new RiskAssessment(
                    0,
                    RiskLevel.NONE,
                    List.of(new DetectedSignal(
                            SignalType.UNREGISTERED_DOMAIN,
                            "Domain is available for registration (not active)",
                            0
                    ))
            );
        }

        return riskScorer.score(signalExtractor.extract(sourceDomain, result));
    }
}