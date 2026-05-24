INSERT INTO ai_image_storage_config (
    storage_code,
    storage_name,
    storage_type,
    endpoint,
    bucket_name,
    base_url,
    root_path,
    extra_config_json,
    enabled,
    is_default,
    remark
) VALUES (
    'postimages_zhouya',
    'Postimages 个人图床',
    'object_storage',
    'https://api.postimage.org/1/upload',
    'zhouya166913',
    'https://i.postimg.cc',
    '',
    '{"profileUrl":"https://postimg.cc/user/zhouya166913","profileUser":"zhouya166913","directImageDomain":"https://i.postimg.cc","apiKeyUrl":"https://postimages.org/login/api","apiStatus":"pending_api_key","resize":"0","expire":"0"}',
    0,
    0,
    'Postimages 个人图床配置；当前缺少 API Key，暂不启用自动上传。拿到 API Key 后再启用并联调上传。'
) ON DUPLICATE KEY UPDATE
    storage_name = VALUES(storage_name),
    storage_type = VALUES(storage_type),
    endpoint = VALUES(endpoint),
    bucket_name = VALUES(bucket_name),
    base_url = VALUES(base_url),
    extra_config_json = VALUES(extra_config_json),
    remark = VALUES(remark);
