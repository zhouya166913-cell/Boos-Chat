ALTER TABLE ai_model
    ADD COLUMN model_type VARCHAR(40) NOT NULL DEFAULT 'chat' COMMENT '模型类型：chat/image_generation/embedding' AFTER display_name;

UPDATE ai_model SET model_type = 'chat' WHERE model_type = '';

ALTER TABLE ai_agent
    ADD COLUMN image_generation_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用图片生成能力' AFTER tools_enabled,
    ADD COLUMN image_model_id BIGINT NULL COMMENT '图片生成模型ID' AFTER image_generation_enabled,
    ADD COLUMN image_api_key_id BIGINT NULL COMMENT '图片生成 API Key ID' AFTER image_model_id,
    ADD COLUMN image_storage_strategy VARCHAR(40) NOT NULL DEFAULT 'local' COMMENT '图片存储策略：local/oss/cos/qiniu/s3' AFTER image_api_key_id,
    ADD KEY idx_ai_agent_image_model_id (image_model_id),
    ADD KEY idx_ai_agent_image_api_key_id (image_api_key_id),
    ADD CONSTRAINT fk_ai_agent_image_model FOREIGN KEY (image_model_id) REFERENCES ai_model (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_ai_agent_image_api_key FOREIGN KEY (image_api_key_id) REFERENCES ai_model_api_key (id) ON DELETE SET NULL;

CREATE TABLE ai_generated_image (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '生成图片ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    agent_id BIGINT NOT NULL COMMENT 'AI ID',
    conversation_id BIGINT NULL COMMENT '会话ID',
    message_id BIGINT NULL COMMENT '关联消息ID',
    provider_id BIGINT NOT NULL COMMENT '图片模型供应商ID',
    model_id BIGINT NOT NULL COMMENT '图片模型ID',
    api_key_id BIGINT NULL COMMENT '图片模型 API Key ID',
    prompt MEDIUMTEXT NOT NULL COMMENT '图片生成提示词',
    negative_prompt MEDIUMTEXT NULL COMMENT '反向提示词',
    image_size VARCHAR(40) NOT NULL DEFAULT '' COMMENT '图片尺寸',
    image_count INT NOT NULL DEFAULT 1 COMMENT '生成数量',
    storage_type VARCHAR(40) NOT NULL DEFAULT 'local' COMMENT '存储类型：local/oss/cos/qiniu/s3',
    source_url TEXT NULL COMMENT '模型返回的原始临时图片地址',
    object_url TEXT NULL COMMENT '图床或对象存储访问地址',
    local_path VARCHAR(500) NOT NULL DEFAULT '' COMMENT '本地保存路径',
    context_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '放入模型上下文的短摘要',
    status VARCHAR(30) NOT NULL DEFAULT 'success' COMMENT '状态：pending/success/failed',
    error_message TEXT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_generated_image_user_created (user_id, create_time),
    KEY idx_ai_generated_image_agent_created (agent_id, create_time),
    KEY idx_ai_generated_image_conversation (conversation_id),
    KEY idx_ai_generated_image_message (message_id),
    KEY idx_ai_generated_image_model (model_id),
    CONSTRAINT fk_ai_generated_image_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_generated_image_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_generated_image_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_generated_image_message FOREIGN KEY (message_id) REFERENCES ai_message (id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_generated_image_provider FOREIGN KEY (provider_id) REFERENCES ai_model_provider (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_generated_image_model FOREIGN KEY (model_id) REFERENCES ai_model (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ai_generated_image_api_key FOREIGN KEY (api_key_id) REFERENCES ai_model_api_key (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成图片记录';
