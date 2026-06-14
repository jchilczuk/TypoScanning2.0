package pl.jchilczuk.typoscanningapi.application.heuristic;

public interface DomainSimilarityService {
    boolean isVerySimilar(String sourceDomain, String candidateDomain);
}