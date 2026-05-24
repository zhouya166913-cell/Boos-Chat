INSERT INTO ai_model_provider (provider_code, provider_name, base_url, auth_type, enabled, remark)
SELECT 'qwen',
       '通义千问 / DashScope',
       'https://dashscope.aliyuncs.com/api/v1',
       'bearer',
       1,
       '阿里云百炼 DashScope，多模态生成与图片编辑接口'
WHERE NOT EXISTS (
    SELECT 1 FROM ai_model_provider WHERE provider_code = 'qwen'
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
       'qwen-image-2.0-pro',
       'Qwen Image Edit Pro',
       'image_edit',
       '/services/aigc/multimodal-generation/generation',
       'paid',
       'https://www.alibabacloud.com/help/en/model-studio/qwen-image-edit-api',
       0,
       0,
       0,
       1,
       1,
       '通义图片编辑推荐模型，支持上传原图后按文字指令修改、增删元素、换风格和细节增强'
FROM ai_model_provider provider
WHERE provider.provider_code = 'qwen'
  AND NOT EXISTS (
      SELECT 1
      FROM ai_model existing
      WHERE existing.provider_id = provider.id
        AND existing.model_name = 'qwen-image-2.0-pro'
  );
