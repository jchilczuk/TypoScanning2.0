package pl.jchilczuk.typoscanningapi.application.usecase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.jchilczuk.typoscanningapi.api.dto.AnalysisJobResponse;
import pl.jchilczuk.typoscanningapi.application.ports.AiAnalyzer;
import pl.jchilczuk.typoscanningapi.application.ports.DnsScanner;
import pl.jchilczuk.typoscanningapi.application.ports.HttpScanner;
import pl.jchilczuk.typoscanningapi.application.ports.TlsScanner;
import pl.jchilczuk.typoscanningapi.api.dto.DetectedSignalResponse;
import pl.jchilczuk.typoscanningapi.api.dto.DomainVariantResultDetailsResponse;
import pl.jchilczuk.typoscanningapi.api.dto.DomainVariantResultListItemResponse;
import pl.jchilczuk.typoscanningapi.api.mappers.DomainVariantResultResponseMapper;
import pl.jchilczuk.typoscanningapi.application.ports.models.AiAssessmentResult;
import pl.jchilczuk.typoscanningapi.application.ports.models.DnsAnalysisResult;
import pl.jchilczuk.typoscanningapi.application.ports.models.HttpAnalysisResult;
import pl.jchilczuk.typoscanningapi.application.ports.models.TlsAnalysisResult;
import pl.jchilczuk.typoscanningapi.application.heuristic.HeuristicAssessmentService;
import pl.jchilczuk.typoscanningapi.domain.valueobject.RiskAssessment;
import pl.jchilczuk.typoscanningapi.application.variant.GeneratedVariant;
import pl.jchilczuk.typoscanningapi.application.variant.VariantGenerator;
import pl.jchilczuk.typoscanningapi.domain.model.AnalysisJob;
import pl.jchilczuk.typoscanningapi.domain.enums.AnalysisStatus;
import pl.jchilczuk.typoscanningapi.domain.model.DomainVariantResult;
import pl.jchilczuk.typoscanningapi.infrastructure.persistence.AnalysisJobRepository;
import pl.jchilczuk.typoscanningapi.infrastructure.persistence.DomainVariantResultRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final DomainVariantResultRepository domainVariantResultRepository;
    private final VariantGenerator variantGenerator;
    private final HeuristicAssessmentService heuristicAssessmentService;
    private final DnsScanner dnsScanner;
    private final HttpScanner httpScanner;
    private final TlsScanner tlsScanner;
    private final AiAnalyzer aiAnalyzer;


    public AnalysisJob createJob(String sourceDomain) {
        log.info("Starting new analysis job for source domain: {}", sourceDomain);

        AnalysisJob job = AnalysisJob.builder()
                .sourceDomain(sourceDomain)
                .status(AnalysisStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        AnalysisJob savedJob = analysisJobRepository.save(job);
        log.debug("Created pending analysis job with ID: {}", savedJob.getId());

        List<GeneratedVariant> generatedVariants = variantGenerator.generate(sourceDomain);
        log.info("Generated {} variants for domain: {}", generatedVariants.size(), sourceDomain);

        log.info("Initiating asynchronous analysis for {} variants...", generatedVariants.size());

        List<CompletableFuture<DomainVariantResult>> variantFutures = generatedVariants.stream()
                .map(variant -> processVariantAsync(variant, savedJob, sourceDomain))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                variantFutures.toArray(new CompletableFuture[0])
        );

        List<DomainVariantResult> results = allFutures.thenApply(v -> {
            log.debug("All async tasks finished. Aggregating results for job ID: {}", savedJob.getId());
            return variantFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        }).join();

        log.info("Starting Batch AI Analysis for job ID: {}", savedJob.getId());
        processAiInBatches(sourceDomain, results);

        log.info("Saving {} analysis results to the database for job ID: {}", results.size(), savedJob.getId());
        domainVariantResultRepository.saveAll(results);

        savedJob.setStatus(AnalysisStatus.COMPLETED);
        savedJob.setFinishedAt(OffsetDateTime.now());
        analysisJobRepository.save(savedJob);

        log.info("Analysis job ID: {} completed successfully.", savedJob.getId());
        return savedJob;
    }

    private void processAiInBatches(String sourceDomain, List<DomainVariantResult> allResults) {
        List<DomainVariantResult> suspiciousVariants = allResults.stream()
                .filter(r -> r.isRegistered() && r.getHeuristicScore() != null && r.getHeuristicScore() >= 3)
                .toList();

        log.info("Filtered {} suspicious domains out of {} total for AI review.", suspiciousVariants.size(), allResults.size());

        int batchSize = 20;

        for (int i = 0; i < suspiciousVariants.size(); i += batchSize) {
            int end = Math.min(i + batchSize, suspiciousVariants.size());
            List<DomainVariantResult> batch = suspiciousVariants.subList(i, end);

            log.debug("Sending batch of {} domains to Gemini AI...", batch.size());

            Map<String, AiAssessmentResult> aiResultsMap = aiAnalyzer.analyzeBatch(sourceDomain, batch);

            for (DomainVariantResult variant : batch) {
                AiAssessmentResult aiResult = aiResultsMap.get(variant.getDomainName());

                if (aiResult != null) {
                    variant.setAiClassification(aiResult.classification());
                    variant.setAiSuspicionLevel(aiResult.suspicionLevel());
                    variant.setAiConfidence(aiResult.confidence());
                    variant.setAiRecommendedAction(aiResult.recommendedAction());
                    variant.setAiExplanation(aiResult.explanation());
                } else {
                    variant.setAiExplanation("AI analysis failed: Domain dropped by model during batch processing.");
                }
            }
        }
    }

    private CompletableFuture<DomainVariantResult> processVariantAsync(GeneratedVariant variant, AnalysisJob job, String sourceDomain) {
        log.debug("Starting async analysis for variant: {} (type: {})", variant.domainName(), variant.variantType());

        return dnsScanner.analyzeAsync(variant.domainName()).thenCompose(dnsResult -> {

            if (!dnsResult.dnsResolved()) {
                log.debug("DNS not resolved for variant: {}. Skipping HTTP/TLS scans (Domain likely inactive).", variant.domainName());

                DomainVariantResult emptyResult = DomainVariantResult.builder()
                        .analysisJob(job)
                        .domainName(variant.domainName())
                        .variantType(variant.variantType())
                        .registered(false)
                        .dnsResolved(false)
                        .hasARecord(false)
                        .hasAaaaRecord(false)
                        .hasMxRecord(dnsResult.hasMxRecord())
                        .httpReachable(false)
                        .httpsReachable(false)
                        .redirectDetected(false)
                        .tlsCertificatePresent(false)
                        .build();

                return CompletableFuture.completedFuture(evaluateRisk(sourceDomain, emptyResult));
            }

            log.debug("DNS resolved for variant: {}. Starting HTTP and TLS scans...", variant.domainName());
            CompletableFuture<HttpAnalysisResult> httpFuture = httpScanner.analyzeAsync(variant.domainName());
            CompletableFuture<TlsAnalysisResult> tlsFuture = tlsScanner.analyzeAsync(variant.domainName());

            return CompletableFuture.allOf(httpFuture, tlsFuture).thenApply(v -> {
                HttpAnalysisResult httpResult = httpFuture.join();
                TlsAnalysisResult tlsResult = tlsFuture.join();

                DomainVariantResult result = DomainVariantResult.builder()
                        .analysisJob(job)
                        .domainName(variant.domainName())
                        .variantType(variant.variantType())
                        .registered(true)
                        .dnsResolved(true)
                        .hasARecord(dnsResult.hasARecord())
                        .hasAaaaRecord(dnsResult.hasAaaaRecord())
                        .hasMxRecord(dnsResult.hasMxRecord())
                        .httpReachable(httpResult.httpReachable())
                        .httpsReachable(httpResult.httpsReachable())
                        .redirectDetected(httpResult.redirectDetected())
                        .finalUrl(httpResult.finalUrl())
                        .pageTitle(httpResult.pageTitle())
                        .htmlSnippet(httpResult.htmlSnippet())
                        .loginFormDetected(httpResult.loginFormDetected())
                        .tlsCertificatePresent(tlsResult.tlsCertificatePresent())
                        .build();

                return evaluateRisk(sourceDomain, result);
            });
        });
    }

    private DomainVariantResult evaluateRisk(String sourceDomain, DomainVariantResult result) {
        RiskAssessment assessment = heuristicAssessmentService.assess(sourceDomain, result);
        result.setHeuristicScore(assessment.score());
        result.setRiskLevel(assessment.riskLevel());

        result.setAiClassification(result.isRegistered() ? "BENIGN" : "INACTIVE");
        result.setAiSuspicionLevel("LOW");
        result.setAiConfidence("HIGH");
        result.setAiRecommendedAction("NO_ACTION");
        result.setAiExplanation(result.isRegistered()
                ? "AI analysis skipped: Technical heuristics indicate no significant threat signals."
                : "AI analysis skipped: The domain is currently not registered or inactive.");

        log.debug("Completed technical analysis for variant: {}. Risk level: {}, Score: {}",
                result.getDomainName(), assessment.riskLevel(), assessment.score());

        return result;
    }

    @Transactional(readOnly = true)
    public List<DomainVariantResultListItemResponse> getResultsForJob(Long analysisJobId) {
        log.debug("Fetching result list for analysis job ID: {}", analysisJobId);
        return domainVariantResultRepository.findByAnalysisJobId(analysisJobId).stream()
                .map(DomainVariantResultResponseMapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnalysisJobResponse> getAllJobs() {
        log.debug("Fetching all analysis jobs");
        return analysisJobRepository.findAll().stream()
                .sorted((j1, j2) -> j2.getCreatedAt().compareTo(j1.getCreatedAt()))
                .map(job -> new AnalysisJobResponse(
                        job.getId(),
                        job.getSourceDomain(),
                        job.getStatus().name(),
                        job.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DomainVariantResultDetailsResponse getResultDetails(Long resultId) {
        log.debug("Fetching details for result ID: {}", resultId);

        DomainVariantResult result = domainVariantResultRepository.findById(resultId)
                .orElseThrow(() -> {
                    log.warn("Result with ID: {} not found", resultId);
                    return new IllegalArgumentException("Result not found: " + resultId);
                });

        RiskAssessment assessment = heuristicAssessmentService.assess(
                result.getAnalysisJob().getSourceDomain(),
                result
        );

        List<DetectedSignalResponse> detectedSignals = assessment.detectedSignals().stream()
                .map(signal -> new DetectedSignalResponse(
                        signal.signalType().name(),
                        signal.description(),
                        signal.scoreContribution()
                ))
                .toList();

        return DomainVariantResultResponseMapper.toDetailsResponse(result, detectedSignals);
    }
}