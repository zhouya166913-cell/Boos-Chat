UPDATE ai_model_provider
SET provider_name = '智谱 AI',
    remark = '智谱 AI 官方文档地址；实际调用接口在模型配置中填写完整 URL。'
WHERE provider_code = 'zhipu';

UPDATE ai_model model
JOIN ai_model_provider provider ON provider.id = model.provider_id
SET model.display_name = '智谱 GLM-Image 图片生成',
    model.official_doc_url = 'https://docs.bigmodel.cn/cn/guide/models/image-generation/glm-image',
    model.remark = '图片生成模型。'
WHERE provider.provider_code = 'zhipu'
  AND model.model_name = 'glm-image';

UPDATE ai_model_api_key api_key
JOIN ai_model model ON model.id = api_key.model_id
JOIN ai_model_provider provider ON provider.id = api_key.provider_id
SET api_key.key_name = '智谱 GLM-Image 图片生成 Key'
WHERE provider.provider_code = 'zhipu'
  AND model.model_name = 'glm-image';

UPDATE ai_image_storage_config
SET storage_name = '蓝图图片 COS',
    storage_type = 'cos',
    remark = '用户上传图片保存到腾讯云 COS'
WHERE storage_code = 'tencent_cos_lantu_1308986692_ap_nanjing';

UPDATE ai_image_storage_config
SET storage_name = '蓝图图片 COS',
    storage_type = 'cos',
    remark = '用户上传图片保存到腾讯云 COS'
WHERE bucket_name = 'lantu-1308986692'
  AND storage_type = 'cos';
