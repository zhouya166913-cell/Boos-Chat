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
SELECT
    provider.id,
    'glm-5v-turbo',
    '智谱多模态工作台',
    'chat',
    '/chat/completions',
    'paid',
    'https://docs.bigmodel.cn/cn/guide/models/vlm/glm-5v-turbo',
    200000,
    1,
    1,
    1,
    1,
    '智谱最新多模态 Coding 基座模型，支持图片、视频、文本和文件输入，适合方案分析、页面截图诊断、资料解读和视觉任务规划'
FROM ai_model_provider provider
WHERE provider.provider_code = 'zhipu'
  AND NOT EXISTS (
      SELECT 1
      FROM ai_model existing
      WHERE existing.provider_id = provider.id
        AND existing.model_name = 'glm-5v-turbo'
  );
