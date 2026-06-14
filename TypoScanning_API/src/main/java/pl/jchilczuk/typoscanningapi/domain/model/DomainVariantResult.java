package pl.jchilczuk.typoscanningapi.domain.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.jchilczuk.typoscanningapi.domain.enums.RiskLevel;
import pl.jchilczuk.typoscanningapi.domain.enums.VariantType;

@Entity
@Table(name = "domain_variant_results")
@Getter
@Setter
@NoArgsConstructor
public class DomainVariantResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_job_id", nullable = false)
    private AnalysisJob analysisJob;

    @Column(nullable = false)
    private String domainName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VariantType variantType;

    @Column(nullable = false)
    private boolean registered;

    @Column(name = "dns_resolved", nullable = false)
    private boolean dnsResolved;

    @Column(name = "has_a_record", nullable = false)
    private boolean hasARecord;

    @Column(name = "has_aaaa_record", nullable = false)
    private boolean hasAaaaRecord;

    @Column(name = "has_mx_record", nullable = false)
    private boolean hasMxRecord;

    @Column(name = "http_reachable", nullable = false)
    private boolean httpReachable;

    @Column(name = "https_reachable", nullable = false)
    private boolean httpsReachable;

    @Column(name = "redirect_detected", nullable = false)
    private boolean redirectDetected;

    @Column(name = "final_url")
    private String finalUrl;

    @Column(name = "page_title")
    private String pageTitle;

    @Column(name = "html_snippet", length = 4000)
    private String htmlSnippet;

    @Column(name = "tls_certificate_present", nullable = false)
    private boolean tlsCertificatePresent;

    @Column(name = "login_form_detected", nullable = false)
    private boolean loginFormDetected;

    @Column(name = "heuristic_score")
    private Integer heuristicScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "ai_classification")
    private String aiClassification;

    @Column(name = "ai_suspicion_level")
    private String aiSuspicionLevel;

    @Column(name = "ai_confidence")
    private String aiConfidence;

    @Column(name = "ai_recommended_action")
    private String aiRecommendedAction;

    @Column(name = "ai_explanation", length = 2000)
    private String aiExplanation;



    @Builder
    public DomainVariantResult(AnalysisJob analysisJob,
                               String domainName,
                               VariantType variantType,
                               boolean registered,
                               boolean dnsResolved,
                               boolean hasARecord,
                               boolean hasAaaaRecord,
                               boolean hasMxRecord,
                               boolean httpReachable,
                               boolean httpsReachable,
                               boolean redirectDetected,
                               String finalUrl,
                               String pageTitle,
                               String htmlSnippet,
                               boolean tlsCertificatePresent,
                               boolean loginFormDetected,
                               Integer heuristicScore,
                               RiskLevel riskLevel,
                               String aiClassification,
                               String aiSuspicionLevel,
                               String aiConfidence,
                               String aiRecommendedAction,
                               String aiExplanation
                               ) {
        this.analysisJob = analysisJob;
        this.domainName = domainName;
        this.variantType = variantType;
        this.registered = registered;
        this.dnsResolved = dnsResolved;
        this.hasARecord = hasARecord;
        this.hasAaaaRecord = hasAaaaRecord;
        this.hasMxRecord = hasMxRecord;
        this.httpReachable = httpReachable;
        this.httpsReachable = httpsReachable;
        this.redirectDetected = redirectDetected;
        this.finalUrl = finalUrl;
        this.pageTitle = pageTitle;
        this.htmlSnippet = htmlSnippet;
        this.tlsCertificatePresent = tlsCertificatePresent;
        this.loginFormDetected = loginFormDetected;
        this.heuristicScore = heuristicScore;
        this.riskLevel = riskLevel;
        this.aiClassification = aiClassification;
        this.aiSuspicionLevel = aiSuspicionLevel;
        this.aiConfidence = aiConfidence;
        this.aiRecommendedAction = aiRecommendedAction;
        this.aiExplanation = aiExplanation;
    }
}