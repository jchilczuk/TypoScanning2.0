CREATE TABLE domain_variant_results (
    id BIGSERIAL PRIMARY KEY,
    analysis_job_id BIGINT NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    variant_type VARCHAR(50) NOT NULL,
    registered BOOLEAN NOT NULL,
    dns_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    has_a_record BOOLEAN NOT NULL DEFAULT FALSE,
    has_aaaa_record BOOLEAN NOT NULL DEFAULT FALSE,
    has_mx_record BOOLEAN NOT NULL DEFAULT FALSE,
    http_reachable BOOLEAN NOT NULL DEFAULT FALSE,
    https_reachable BOOLEAN NOT NULL DEFAULT FALSE,
    redirect_detected BOOLEAN NOT NULL DEFAULT FALSE,
    final_url VARCHAR(1000),
    page_title VARCHAR(1000),
    html_snippet VARCHAR(4000),
    tls_certificate_present BOOLEAN NOT NULL DEFAULT FALSE,
    login_form_detected BOOLEAN NOT NULL DEFAULT FALSE,
    heuristic_score INTEGER,
    risk_level VARCHAR(50),
    CONSTRAINT fk_domain_variant_results_analysis_job
        FOREIGN KEY (analysis_job_id) REFERENCES analysis_jobs(id)
);