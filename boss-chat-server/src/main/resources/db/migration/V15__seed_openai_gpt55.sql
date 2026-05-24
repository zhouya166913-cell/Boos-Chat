INSERT INTO ai_model_provider (provider_code, provider_name, base_url, auth_type, enabled, remark)
SELECT 'openai',
       'OpenAI',
       'https://api.openai.com/v1',
       'bearer',
       1,
       'OpenAI API, supports Chat Completions and Responses API'
WHERE NOT EXISTS (
    SELECT 1 FROM ai_model_provider WHERE provider_code = 'openai'
);

INSERT INTO ai_model (
    provider_id,
    model_name,
    display_name,
    model_type,
    api_path,
    billing_type,
    official_doc_url,
    context_window,
    supports_stream,
    supports_tools,
    supports_vision,
    enabled,
    remark
)
SELECT provider.id,
       'gpt-5.5',
       'GPT-5.5',
       'chat',
       '/chat/completions',
       'paid',
       'https://developers.openai.com/api/docs/models/gpt-5.5/',
       1050000,
       1,
       1,
       1,
       1,
       'OpenAI flagship model for complex reasoning, coding, tool-heavy agents, and vision input'
FROM ai_model_provider provider
WHERE provider.provider_code = 'openai'
  AND NOT EXISTS (
      SELECT 1
      FROM ai_model existing
      WHERE existing.provider_id = provider.id
        AND existing.model_name = 'gpt-5.5'
  );
