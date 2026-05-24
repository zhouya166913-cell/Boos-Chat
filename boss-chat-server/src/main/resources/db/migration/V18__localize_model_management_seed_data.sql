UPDATE ai_agent
SET agent_name = '免费测试助手',
    description = '使用 GLM-4.5-Flash 免费模型进行本地测试。'
WHERE agent_name = 'Free Test Assistant'
   OR description = 'Uses GLM-4.5-Flash free model for local testing.';

UPDATE ai_model_provider
SET provider_name = CASE provider_code
        WHEN 'zhipu' THEN '智谱 AI'
        WHEN 'qwen' THEN '通义千问 / DashScope'
        ELSE provider_name
    END,
    remark = CASE provider_code
        WHEN 'zhipu' THEN '智谱开放平台通用 API，兼容 Chat Completions'
        WHEN 'kimi' THEN '月之暗面 Moonshot API，兼容 OpenAI Chat Completions'
        WHEN 'qwen' THEN '阿里云 DashScope 多模态生成与图片编辑 API'
        WHEN 'openai' THEN 'OpenAI API，支持 Chat Completions 与 Responses API'
        ELSE remark
    END
WHERE provider_code IN ('zhipu', 'kimi', 'qwen', 'openai');

UPDATE ai_model model
JOIN ai_model_provider provider ON provider.id = model.provider_id
SET model.remark = CASE
        WHEN provider.provider_code = 'zhipu' AND model.model_name = 'glm-4.7'
            THEN '智谱复杂推理与智能体编程模型'
        WHEN provider.provider_code = 'zhipu' AND model.model_name = 'glm-4.5-flash'
            THEN '智谱免费高效模型，适合基础对话和轻量任务'
        WHEN provider.provider_code = 'zhipu' AND model.model_name = 'glm-5v-turbo'
            THEN '智谱多模态模型，支持图片、视频、文本和文件输入'
        WHEN provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6'
            THEN 'Kimi 官方模型，兼容 OpenAI API 格式'
        WHEN provider.provider_code = 'zhipu' AND model.model_name = 'glm-image'
            THEN '智谱图片生成模型，用于 generate_image 工具'
        WHEN provider.provider_code = 'qwen' AND model.model_name = 'qwen-image-2.0-pro'
            THEN '通义图片编辑模型，支持按文字指令编辑图片'
        WHEN provider.provider_code = 'openai' AND model.model_name = 'gpt-5.5'
            THEN 'OpenAI 旗舰模型，适合复杂推理、代码、工具调用和视觉输入'
        ELSE model.remark
    END
WHERE provider.provider_code IN ('zhipu', 'kimi', 'qwen', 'openai');

UPDATE ai_model_api_key
SET remark = '本地种子 Key，可在模型管理中替换'
WHERE remark = 'Local seed key; replace it in model management';

UPDATE ai_model_api_key
SET remark = '可选本地种子 Key，可在模型管理中替换'
WHERE remark = 'Optional local seed key; replace it in model management';
