package pl.jchilczuk.typoscanningapi.application.ports;

import pl.jchilczuk.typoscanningapi.application.ports.models.AiAssessmentResult;
import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;

import java.util.List;
import java.util.Map;

public interface AiAnalyzer {

    /**
     * Analyzes a batch of suspicious domain variants using an AI model.
     *
     * @param sourceDomain The original domain that is potentially being squatted.
     * @param batch A list of domain variants that crossed the risk threshold.
     * @return A map where the key is the domain name, and the value is the AI's assessment result.
     */
    Map<String, AiAssessmentResult> analyzeBatch(String sourceDomain, List<DomainVariantResult> batch);

}