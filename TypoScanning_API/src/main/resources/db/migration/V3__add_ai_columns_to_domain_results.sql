ALTER TABLE domain_variant_results
ADD COLUMN ai_classification VARCHAR(255),
ADD COLUMN ai_suspicion_level VARCHAR(255),
ADD COLUMN ai_confidence VARCHAR(255),
ADD COLUMN ai_recommended_action VARCHAR(255),
ADD COLUMN ai_explanation VARCHAR(2000);