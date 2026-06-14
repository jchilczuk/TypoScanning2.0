package pl.jchilczuk.typoscanningapi.domain.valueobject;

import pl.jchilczuk.typoscanningapi.domain.enums.SignalType;

public record DetectedSignal(
        SignalType signalType,
        String description,
        int scoreContribution
) {
}