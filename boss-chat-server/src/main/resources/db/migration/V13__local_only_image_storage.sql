UPDATE ai_image_storage_config
SET enabled = 1,
    is_default = 1,
    storage_type = 'local',
    endpoint = '',
    region = '',
    bucket_name = '',
    base_url = '',
    access_key_id_cipher = NULL,
    access_key_id_mask = '',
    access_key_secret_cipher = NULL,
    access_key_secret_mask = '',
    extra_config_json = '',
    remark = '当前阶段仅使用本地图片存储测试'
WHERE storage_code = 'local_dev';

UPDATE ai_agent
SET image_storage_strategy = 'local',
    image_storage_config_id = (SELECT id FROM ai_image_storage_config WHERE storage_code = 'local_dev' LIMIT 1)
WHERE image_storage_config_id IS NULL
   OR image_storage_config_id IN (
       SELECT id FROM ai_image_storage_config WHERE storage_code <> 'local_dev'
   );

DELETE FROM ai_image_storage_config
WHERE storage_code <> 'local_dev';
