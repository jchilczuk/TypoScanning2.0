package pl.jchilczuk.typoscanningapi.api.mappers;

import pl.jchilczuk.typoscanningapi.api.dto.DetectedSignalResponse;
import pl.jchilczuk.typoscanningapi.api.dto.DomainVariantResultDetailsResponse;
import pl.jchilczuk.typoscanningapi.api.dto.DomainVariantResultListItemResponse;
import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;

import java.util.List;

public final class DomainVariantResultResponseMapper {

    private DomainVariantResultResponseMapper() {
    }

    public static DomainVariantResultListItemResponse toListItemResponse(DomainVariantResult result) {
        return new DomainVariantResultListItemResponse(
                result.getId(),
                result.getDomainName(),
                result.getVariantType() != null ? result.getVariantType().name() : null,
                result.getHeuristicScore(),
                result.getRiskLevel() != null ? result.getRiskLevel().name() : null,
                result.getAiClassification(),
                result.getAiSuspicionLevel(),
                result.getAiConfidence(),
                result.getAiRecommendedAction(),
                result.getAiExplanation()
        );
    }

    public static DomainVariantResultDetailsResponse toDetailsResponse(
            DomainVariantResult result,
            List<DetectedSignalResponse> detectedSignals
    ) {
        return new DomainVariantResultDetailsResponse(
                result.getId(),
                result.getDomainName(),
                result.getVariantType().name(),
                result.isRegistered(),
                result.isDnsResolved(),
                result.isHasARecord(),
                result.isHasAaaaRecord(),
                result.isHasMxRecord(),
                result.isHttpReachable(),
                result.isHttpsReachable(),
                result.isRedirectDetected(),
                result.getFinalUrl(),
                result.getPageTitle(),
                result.getHtmlSnippet(),
                result.isTlsCertificatePresent(),
                result.isLoginFormDetected(),
                result.getHeuristicScore(),
                result.getRiskLevel() != null ? result.getRiskLevel().name() : null,
                detectedSignals,
                result.getAiClassification(),
                result.getAiSuspicionLevel(),
                result.getAiConfidence(),
                result.getAiRecommendedAction(),
                result.getAiExplanation()
        );
    }
}