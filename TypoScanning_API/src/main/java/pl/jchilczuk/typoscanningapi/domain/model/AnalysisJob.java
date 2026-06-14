package pl.jchilczuk.typoscanningapi.domain.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.jchilczuk.typoscanningapi.domain.enums.AnalysisStatus;

import java.time.OffsetDateTime;

@Entity
@Table(name = "analysis_jobs")
@Getter
@Setter
@NoArgsConstructor
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceDomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime finishedAt;

    @Builder
    public AnalysisJob(String sourceDomain, AnalysisStatus status, OffsetDateTime createdAt, OffsetDateTime finishedAt) {
        this.sourceDomain = sourceDomain;
        this.status = status;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
    }
}