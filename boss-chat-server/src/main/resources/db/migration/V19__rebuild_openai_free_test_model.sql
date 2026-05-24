UPDATE ai_agent
SET enabled = 0,
    model_id = NULL,
    api_key_id = NULL,
    tools_enabled = 0
WHERE model_provider = 'openai'
   OR model_name LIKE 'gpt-%'
   OR agent_name IN ('ChatGPT', 'Free Test Assistant');

DELETE FROM ai_model_provider
WHERE provider_code = 'openai';

INSERT INTO ai_model_provider (provider_code, provider_name, base_url, auth_type, enabled, remark)
VALUES (
    'openai',
    'OpenAI 免费测试',
    'https://api.openai.com/v1',
    'bearer',
    1,
    'OpenAI API 免费测试配置，用于基础对话链路验证'
);

INSERT INTO ai_model (
    provider_id,
    model_name,
    display_name,
    model_type,
    api_path,
    billing_type,
    official_doc_url,
    compatibility_profile,
    context_window,
    supports_stream,
    supports_tools,
    supports_vision,
    enabled,
    remark
)
SELECT provider.id,
       'gpt-5.5',
       'ChatGPT 免费测试',
       'chat',
       '/chat/completions',
       'free',
       'https://developers.openai.com/api/docs/models/gpt-5.5/',
       'openai_gpt5',
       1050000,
       1,
       0,
       0,
       1,
       'OpenAI 免费测试模型，用于基础对话链路验证'
FROM ai_model_provider provider
WHERE provider.provider_code = 'openai';
