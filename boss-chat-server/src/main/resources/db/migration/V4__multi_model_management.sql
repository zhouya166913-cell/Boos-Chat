INSERT INTO ai_model_provider (provider_code, provider_name, base_url, auth_type, enabled, remark)
VALUES
    ('zhipu', '智谱 AI', 'https://open.bigmodel.cn/api/paas/v4', 'bearer', 1, '智谱开放平台通用 API，兼容 Chat Completions'),
    ('kimi', 'Kimi / Moonshot', 'https://api.moonshot.cn/v1', 'bearer', 1, 'Kimi 开放平台，兼容 OpenAI Chat Completions 协议');

INSERT INTO ai_model (provider_id, model_name, display_name, context_window, supports_stream, supports_tools, supports_vision, enabled, remark)
SELECT id, 'glm-4.7', 'GLM-4.7', 200000, 1, 1, 0, 1, '智谱面向复杂推理和 Agentic Coding 强化的高智能模型'
FROM ai_model_provider WHERE provider_code = 'zhipu';

INSERT INTO ai_model (provider_id, model_name, display_name, context_window, supports_stream, supports_tools, supports_vision, enabled, remark)
SELECT id, 'glm-4.5-flash', 'GLM-4.5-Flash', 128000, 1, 1, 0, 1, '智谱免费高效模型，适合基础对话和轻量任务'
FROM ai_model_provider WHERE provider_code = 'zhipu';

INSERT INTO ai_model (provider_id, model_name, display_name, context_window, supports_stream, supports_tools, supports_vision, enabled, remark)
SELECT id, 'kimi-k2.6', 'Kimi K2.6', 256000, 1, 1, 1, 1, 'Kimi 官方快速开始示例模型，兼容 OpenAI API 格式'
FROM ai_model_provider WHERE provider_code = 'kimi';
