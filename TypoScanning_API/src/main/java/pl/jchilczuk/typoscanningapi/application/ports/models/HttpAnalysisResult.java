package pl.jchilczuk.typoscanningapi.application.ports.models;

public record HttpAnalysisResult(
        boolean httpReachable,
        boolean httpsReachable,
        boolean redirectDetected,
        String finalUrl,
        String pageTitle,
        String htmlSnippet,
        boolean loginFormDetected
) {
}