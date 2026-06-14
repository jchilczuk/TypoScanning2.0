package pl.jchilczuk.typoscanningapi.application.heuristic;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

@Service
public class SimpleDomainSimilarityService implements DomainSimilarityService {

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    @Override
    public boolean isVerySimilar(String sourceDomain, String candidateDomain) {
        String sourceBase = extractBaseDomain(sourceDomain).toLowerCase();
        String candidateBase = extractBaseDomain(candidateDomain).toLowerCase();

        if (sourceBase.equals(candidateBase)) {
            return true;
        }

        Integer distance = levenshteinDistance.apply(sourceBase, candidateBase);
        if (distance != null && distance <= 2) {
            return true;
        }

        return candidateBase.contains(sourceBase) || sourceBase.contains(candidateBase);
    }

    private String extractBaseDomain(String domain) {
        String[] parts = domain.split("\\.");
        return parts.length > 0 ? parts[0] : domain;
    }
}