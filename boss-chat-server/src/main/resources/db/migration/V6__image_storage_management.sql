CREATE TABLE ai_image_storage_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片存储配置ID',
    storage_code VARCHAR(80) NOT NULL COMMENT '存储编码，例如 local_dev/aliyun_oss/qiniu',
    storage_name VARCHAR(120) NOT NULL COMMENT '存储名称',
    storage_type VARCHAR(40) NOT NULL DEFAULT 'local' COMMENT '存储类型：local/object_storage/oss/cos/qiniu/s3/custom',
    endpoint VARCHAR(255) NOT NULL DEFAULT '' COMMENT '服务端点，例如 OSS Endpoint 或 S3 Endpoint',
    region VARCHAR(80) NOT NULL DEFAULT '' COMMENT '区域',
    bucket_name VARCHAR(160) NOT NULL DEFAULT '' COMMENT 'Bucket 或空间名称',
    base_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '公开访问域名或 CDN 地址',
    root_path VARCHAR(255) NOT NULL DEFAULT '' COMMENT '根目录或保存前缀',
    access_key_id_cipher TEXT NULL COMMENT '加密后的 AccessKeyId',
    access_key_id_mask VARCHAR(80) NOT NULL DEFAULT '' COMMENT '脱敏 AccessKeyId',
    access_key_secret_cipher TEXT NULL COMMENT '加密后的 AccessKeySecret',
    access_key_secret_mask VARCHAR(80) NOT NULL DEFAULT '' COMMENT '脱敏 AccessKeySecret',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认图片存储',
    remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_image_storage_code (storage_code),
    KEY idx_ai_image_storage_enabled (enabled),
    KEY idx_ai_image_storage_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片存储与图床配置';

ALTER TABLE ai_agent
    ADD COLUMN image_storage_config_id BIGINT NULL COMMENT '图片存储配置ID' AFTER image_storage_strategy,
    ADD KEY idx_ai_agent_image_storage_config_id (image_storage_config_id),
    ADD CONSTRAINT fk_ai_agent_image_storage_config FOREIGN KEY (image_storage_config_id) REFERENCES ai_image_storage_config (id) ON DELETE SET NULL;

INSERT INTO ai_image_storage_config (
    storage_code,
    storage_name,
    storage_type,
    root_path,
    enabled,
    is_default,
    remark
) VALUES (
    'local_dev',
    '本地开发存储',
    'local',
    'uploads/ai-images',
    1,
    1,
    '开发阶段默认保存到 boss-chat-server/uploads/ai-images，正式环境建议切换为 OSS/COS/七牛/S3'
);
