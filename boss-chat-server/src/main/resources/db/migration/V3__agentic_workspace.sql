CREATE TABLE ai_memory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记忆ID',
    agent_id BIGINT NOT NULL COMMENT '所属智能体ID',
    memory_type VARCHAR(40) NOT NULL DEFAULT 'profile' COMMENT '记忆类型',
    memory_key VARCHAR(120) NOT NULL COMMENT '记忆键',
    memory_value MEDIUMTEXT NOT NULL COMMENT '记忆内容',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_memory_agent_key (agent_id, memory_key),
    KEY idx_ai_memory_agent_enabled (agent_id, enabled),
    CONSTRAINT fk_ai_memory_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体长期记忆';

CREATE TABLE ai_knowledge_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识文档ID',
    agent_id BIGINT NOT NULL COMMENT '所属智能体ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content MEDIUMTEXT NOT NULL COMMENT '正文',
    tags VARCHAR(255) NOT NULL DEFAULT '' COMMENT '标签',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ai_knowledge_agent_enabled (agent_id, enabled),
    CONSTRAINT fk_ai_knowledge_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体知识库';

CREATE TABLE ai_workflow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工作流ID',
    agent_id BIGINT NOT NULL COMMENT '所属智能体ID',
    workflow_code VARCHAR(80) NOT NULL COMMENT '工作流编码',
    workflow_name VARCHAR(120) NOT NULL COMMENT '工作流名称',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '工作流说明',
    definition_json MEDIUMTEXT NOT NULL COMMENT '工作流定义JSON',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_workflow_agent_code (agent_id, workflow_code),
    KEY idx_ai_workflow_agent_enabled (agent_id, enabled),
    CONSTRAINT fk_ai_workflow_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体工作流';

CREATE TABLE ai_tool_execution (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工具执行ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    agent_id BIGINT NOT NULL COMMENT '智能体ID',
    tool_name VARCHAR(120) NOT NULL COMMENT '工具名称',
    arguments_json MEDIUMTEXT NOT NULL COMMENT '工具参数',
    result_summary MEDIUMTEXT NOT NULL COMMENT '执行结果摘要',
    status VARCHAR(30) NOT NULL DEFAULT 'success' COMMENT '执行状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_ai_tool_execution_user_created (user_id, create_time),
    KEY idx_ai_tool_execution_agent_created (agent_id, create_time),
    CONSTRAINT fk_ai_tool_execution_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_tool_execution_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体工具执行记录';