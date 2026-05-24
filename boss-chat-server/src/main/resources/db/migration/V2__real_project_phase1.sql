CREATE TABLE ai_model_provider (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模型供应商ID',
    provider_code VARCHAR(80) NOT NULL COMMENT '供应商编码，例如 zhipu/kimi/openai',
    provider_name VARCHAR(120) NOT NULL COMMENT '供应商名称',
    base_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'API Base URL，不包含 /chat/completions',
    auth_type VARCHAR(40) NOT NULL DEFAULT 'bearer' COMMENT '鉴权方式，当前默认 bearer',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_model_provider_code (provider_code),
    KEY idx_ai_model_provider_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型供应商';

CREATE TABLE ai_model (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '模型ID',
    provider_id BIGINT NOT NULL COMMENT '供应商ID',
    model_name VARCHAR(120) NOT NULL COMMENT '模型调用名称',
    display_name VARCHAR(120) NOT NULL COMMENT '模型显示名称',
    context_window INT NOT NULL DEFAULT 0 COMMENT '上下文窗口，0 表示暂未维护',
    supports_stream TINYINT NOT NULL DEFAULT 1 COMMENT '是否支持流式输出',
    supports_tools TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持工具调用',
    supports_vision TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持视觉',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_model_provider_model (provider_id, model_name),
    KEY idx_ai_model_provider_enabled (provider_id, enabled),
    CONSTRAINT fk_ai_model_provider FOREIGN KEY (provider_id) REFERENCES ai_model_provider (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型列表';

CREATE TABLE ai_model_api_key (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'API Key ID',
    provider_id BIGINT NOT NULL COMMENT '供应商ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    key_name VARCHAR(120) NOT NULL COMMENT 'Key 名称',
    key_type VARCHAR(40) NOT NULL DEFAULT 'paid' COMMENT 'Key 类型：paid/free/company/personal',
    api_key_cipher TEXT NOT NULL COMMENT '加密后的 API Key',
    api_key_mask VARCHAR(80) NOT NULL DEFAULT '' COMMENT '脱敏展示值',
    priority INT NOT NULL DEFAULT 100 COMMENT '优先级，数字越小越优先',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_model_api_key_provider_enabled (provider_id, enabled),
    KEY idx_ai_model_api_key_model_enabled (model_id, enabled),
    CONSTRAINT fk_ai_model_api_key_provider FOREIGN KEY (provider_id) REFERENCES ai_model_provider (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_model_api_key_model FOREIGN KEY (model_id) REFERENCES ai_model (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型 API Key';

CREATE TABLE ai_agent (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
    agent_code VARCHAR(80) NOT NULL COMMENT '智能体编码',
    agent_name VARCHAR(120) NOT NULL COMMENT '智能体名称',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '智能体说明',
    system_prompt MEDIUMTEXT NOT NULL COMMENT '系统提示词',
    model_provider VARCHAR(60) NOT NULL DEFAULT '' COMMENT '兼容字段：模型供应商',
    model_name VARCHAR(120) NOT NULL DEFAULT '' COMMENT '兼容字段：模型名称',
    model_id BIGINT NULL COMMENT '模型ID',
    api_key_id BIGINT NULL COMMENT 'API Key ID',
    temperature DECIMAL(4, 2) NOT NULL DEFAULT 0.35 COMMENT '温度',
    max_completion_tokens INT NOT NULL DEFAULT 4096 COMMENT '最大输出Token',
    memory_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用长期记忆',
    knowledge_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用知识库',
    workflow_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用工作流',
    tools_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用本地工具调用',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_agent_code (agent_code),
    KEY idx_ai_agent_enabled (enabled),
    KEY idx_ai_agent_model_id (model_id),
    KEY idx_ai_agent_api_key_id (api_key_id),
    CONSTRAINT fk_ai_agent_model FOREIGN KEY (model_id) REFERENCES ai_model (id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_agent_api_key FOREIGN KEY (api_key_id) REFERENCES ai_model_api_key (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体';

CREATE TABLE ai_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '对话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    agent_id BIGINT NOT NULL COMMENT '智能体ID',
    title VARCHAR(120) NOT NULL DEFAULT '' COMMENT '对话标题',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/archived/deleted',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_conversation_user_updated (user_id, update_time),
    KEY idx_ai_conversation_agent (agent_id),
    CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_conversation_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话';

CREATE TABLE ai_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色：system/user/assistant/tool',
    content MEDIUMTEXT NOT NULL COMMENT '消息内容',
    model_provider VARCHAR(60) NOT NULL DEFAULT '' COMMENT '模型供应商编码',
    model_name VARCHAR(120) NOT NULL DEFAULT '' COMMENT '模型名称',
    prompt_tokens INT NOT NULL DEFAULT 0 COMMENT '输入Token',
    completion_tokens INT NOT NULL DEFAULT 0 COMMENT '输出Token',
    total_tokens INT NOT NULL DEFAULT 0 COMMENT '总Token',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_message_conversation_created (conversation_id, create_time),
    KEY idx_ai_message_user_created (user_id, create_time),
    CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_message_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 消息';

CREATE TABLE ai_usage_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用量记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    agent_id BIGINT NOT NULL COMMENT '智能体ID',
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    model_provider VARCHAR(60) NOT NULL DEFAULT '' COMMENT '模型供应商编码',
    model_name VARCHAR(120) NOT NULL DEFAULT '' COMMENT '模型名称',
    prompt_tokens INT NOT NULL DEFAULT 0 COMMENT '输入Token',
    completion_tokens INT NOT NULL DEFAULT 0 COMMENT '输出Token',
    total_tokens INT NOT NULL DEFAULT 0 COMMENT '总Token',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_usage_user_created (user_id, create_time),
    KEY idx_ai_usage_agent_created (agent_id, create_time),
    CONSTRAINT fk_ai_usage_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_usage_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_usage_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用用量';
