package pl.jchilczuk.typoscanningapi.application.heuristic;

import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;
import pl.jchilczuk.typoscanningapi.domain.valueobject.DetectedSignal;

import java.util.List;

public interface SignalExtractor {
    List<DetectedSignal> extract(String sourceDomain, DomainVariantResult result);
}