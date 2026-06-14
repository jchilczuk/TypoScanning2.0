package pl.jchilczuk.typoscanningapi.application.ports.models;

public record TlsAnalysisResult(
        boolean tlsCertificatePresent
) {
}