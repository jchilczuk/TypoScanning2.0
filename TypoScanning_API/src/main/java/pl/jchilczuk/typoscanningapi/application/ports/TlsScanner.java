package pl.jchilczuk.typoscanningapi.application.ports;

import pl.jchilczuk.typoscanningapi.application.ports.models.TlsAnalysisResult;
import java.util.concurrent.CompletableFuture;

public interface TlsScanner {

    CompletableFuture<TlsAnalysisResult> analyzeAsync(String domainName);
}