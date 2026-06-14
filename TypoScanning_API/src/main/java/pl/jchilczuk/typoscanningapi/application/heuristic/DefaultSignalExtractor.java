package pl.jchilczuk.typoscanningapi.application.heuristic;

import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;
import pl.jchilczuk.typoscanningapi.domain.enums.SignalType;
import pl.jchilczuk.typoscanningapi.domain.valueobject.DetectedSignal;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultSignalExtractor implements SignalExtractor {

    private final DomainSimilarityService domainSimilarityService;

    public DefaultSignalExtractor(DomainSimilarityService domainSimilarityService) {
        this.domainSimilarityService = domainSimilarityService;
    }

    @Override
    public List<DetectedSignal> extract(String sourceDomain, DomainVariantResult result) {
        List<DetectedSignal> signals = new ArrayList<>();

        if (domainSimilarityService.isVerySimilar(sourceDomain, result.getDomainName())) {
            signals.add(new DetectedSignal(
                    SignalType.DOMAIN_VERY_SIMILAR,
                    "Domain name is very similar to the source domain",
                    1
            ));
        }

        if (result.isHttpReachable() || result.isHttpsReachable()) {
            signals.add(new DetectedSignal(
                    SignalType.WEB_ACTIVE,
                    "Domain is reachable over HTTP or HTTPS",
                    2
            ));
        }

        if (result.isLoginFormDetected()) {
            signals.add(new DetectedSignal(
                    SignalType.LOGIN_FORM_DETECTED,
                    "Potential login form detected on the page",
                    4
            ));
        }

        if (result.isHasMxRecord()) {
            signals.add(new DetectedSignal(
                    SignalType.MX_PRESENT,
                    "MX record is present",
                    2
            ));
        }

        if (result.isRedirectDetected()) {
            signals.add(new DetectedSignal(
                    SignalType.REDIRECT_DETECTED,
                    "Redirect detected during HTTP/HTTPS analysis",
                    2
            ));
        }

        boolean activeInfrastructure =
                result.isDnsResolved()
                        || result.isHasARecord()
                        || result.isHasAaaaRecord()
                        || result.isHasMxRecord()
                        || result.isHttpReachable()
                        || result.isHttpsReachable()
                        || result.isTlsCertificatePresent();

        boolean hasMeaningfulContent = hasMeaningfulContent(result.getHtmlSnippet());

        if (activeInfrastructure && !hasMeaningfulContent) {
            signals.add(new DetectedSignal(
                    SignalType.ACTIVE_INFRASTRUCTURE_NO_CONTENT,
                    "Active infrastructure detected, but no meaningful page content found",
                    1
            ));
        }

        return signals;
    }

    private boolean hasMeaningfulContent(String htmlSnippet) {
        if (htmlSnippet == null || htmlSnippet.isBlank()) {
            return false;
        }

        String normalized = htmlSnippet.trim().replaceAll("\\s+", " ");
        return normalized.length() >= 30;
    }
}