ALTER TABLE ai_model
    ADD COLUMN compatibility_profile VARCHAR(80) NOT NULL DEFAULT '' COMMENT 'Request compatibility profile, e.g. openai_gpt5/kimi_k2' AFTER official_doc_url;

UPDATE ai_model model
JOIN ai_model_provider provider ON model.provider_id = provider.id
SET model.compatibility_profile = 'openai_gpt5'
WHERE provider.provider_code = 'openai'
  AND LOWER(model.model_name) LIKE 'gpt-5%';

UPDATE ai_model model
JOIN ai_model_provider provider ON model.provider_id = provider.id
SET model.compatibility_profile = 'kimi_k2'
WHERE provider.provider_code = 'kimi'
  AND LOWER(model.model_name) LIKE 'kimi-k2%';
