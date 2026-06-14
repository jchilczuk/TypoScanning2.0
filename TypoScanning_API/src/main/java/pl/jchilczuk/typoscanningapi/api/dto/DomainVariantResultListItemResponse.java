package pl.jchilczuk.typoscanningapi.api.dto;

public record DomainVariantResultListItemResponse(
        Long resultId,
        String domainName,
        String variantType,
        Integer heuristicScore,
        String riskLevel,
        String aiClassification,
        String aiSuspicionLevel,
        String aiConfidence,
        String aiRecommendedAction,
        String aiExplanation
) {
}