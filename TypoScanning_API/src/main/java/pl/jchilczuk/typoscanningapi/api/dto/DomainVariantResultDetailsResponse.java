package pl.jchilczuk.typoscanningapi.api.dto;

import java.util.List;

public record DomainVariantResultDetailsResponse(
        Long resultId,
        String domainName,
        String variantType,
        boolean registered,
        boolean dnsResolved,
        boolean hasARecord,
        boolean hasAaaaRecord,
        boolean hasMxRecord,
        boolean httpReachable,
        boolean httpsReachable,
        boolean redirectDetected,
        String finalUrl,
        String pageTitle,
        String htmlSnippet,
        boolean tlsCertificatePresent,
        boolean loginFormDetected,
        Integer heuristicScore,
        String riskLevel,
        List<DetectedSignalResponse> detectedSignals,
        String aiClassification,
        String aiSuspicionLevel,
        String aiConfidence,
        String aiRecommendedAction,
        String aiExplanation
) {
}