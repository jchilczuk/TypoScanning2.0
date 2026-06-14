package pl.jchilczuk.typoscanningapi.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pl.jchilczuk.typoscanningapi.api.dto.CreateAnalysisRequest;
import pl.jchilczuk.typoscanningapi.api.dto.AnalysisJobResponse;
import pl.jchilczuk.typoscanningapi.api.dto.DomainVariantResultDetailsResponse;
import pl.jchilczuk.typoscanningapi.api.dto.DomainVariantResultListItemResponse;
import pl.jchilczuk.typoscanningapi.application.usecase.AnalysisJobService;
import pl.jchilczuk.typoscanningapi.domain.model.AnalysisJob;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisJobService analysisJobService;

    @PostMapping
    public AnalysisJobResponse createAnalysis(@Valid @RequestBody CreateAnalysisRequest request) {
        AnalysisJob job = analysisJobService.createJob(request.sourceDomain());

        return new AnalysisJobResponse(
                job.getId(),
                job.getSourceDomain(),
                job.getStatus().name(),
                job.getCreatedAt()
        );
    }

    @GetMapping
    public List<AnalysisJobResponse> getAllJobs() {
        return analysisJobService.getAllJobs();
    }

    @GetMapping("/{analysisJobId}/results")
    public List<DomainVariantResultListItemResponse> getResultsForJob(@PathVariable Long analysisJobId) {
        return analysisJobService.getResultsForJob(analysisJobId);
    }

    @GetMapping("/results/{resultId}")
    public DomainVariantResultDetailsResponse getResultDetails(@PathVariable Long resultId) {
        return analysisJobService.getResultDetails(resultId);
    }
}