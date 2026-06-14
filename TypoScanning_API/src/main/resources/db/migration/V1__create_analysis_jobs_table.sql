CREATE TABLE analysis_jobs (
    id BIGSERIAL PRIMARY KEY,
    source_domain VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE
);