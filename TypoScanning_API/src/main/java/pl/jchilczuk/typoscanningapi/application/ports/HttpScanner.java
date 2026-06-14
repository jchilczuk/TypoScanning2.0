package pl.jchilczuk.typoscanningapi.application.ports;

import pl.jchilczuk.typoscanningapi.application.ports.models.HttpAnalysisResult;
import java.util.concurrent.CompletableFuture;

public interface HttpScanner {

    CompletableFuture<HttpAnalysisResult> analyzeAsync(String domainName);
}