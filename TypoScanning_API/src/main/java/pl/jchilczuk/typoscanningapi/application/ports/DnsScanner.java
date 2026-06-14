package pl.jchilczuk.typoscanningapi.application.ports;

import pl.jchilczuk.typoscanningapi.application.ports.models.DnsAnalysisResult;
import java.util.concurrent.CompletableFuture;

public interface DnsScanner {

    CompletableFuture<DnsAnalysisResult> analyzeAsync(String domainName);
}