CREATE TABLE ai_scene (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '场景ID',
    scene_code VARCHAR(80) NOT NULL COMMENT '场景编码',
    scene_name VARCHAR(120) NOT NULL COMMENT '场景名称',
    description VARCHAR(500) NOT NULL DEFAULT '' COMMENT '场景说明',
    chat_mode VARCHAR(20) NOT NULL DEFAULT 'single' COMMENT '会话模式：single 单聊 / team 团队',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_scene_code (scene_code),
    KEY idx_ai_scene_enabled (enabled),
    KEY idx_ai_scene_mode (chat_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 场景';

CREATE TABLE ai_scene_agent (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '场景AI关系ID',
    scene_id BIGINT NOT NULL COMMENT '场景ID',
    agent_id BIGINT NOT NULL COMMENT 'AI ID',
    role_name VARCHAR(120) NOT NULL DEFAULT '' COMMENT '场景中的角色名称',
    sort_order INT NOT NULL DEFAULT 100 COMMENT '排序值',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_scene_agent (scene_id, agent_id),
    KEY idx_ai_scene_agent_scene_enabled (scene_id, enabled),
    KEY idx_ai_scene_agent_agent (agent_id),
    CONSTRAINT fk_ai_scene_agent_scene FOREIGN KEY (scene_id) REFERENCES ai_scene (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_scene_agent_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场景AI';

ALTER TABLE ai_conversation
    ADD COLUMN scene_id BIGINT NULL COMMENT '场景ID' AFTER user_id,
    ADD COLUMN chat_mode VARCHAR(20) NOT NULL DEFAULT 'single' COMMENT '会话模式：single/team' AFTER scene_id,
    ADD KEY idx_ai_conversation_scene_mode (scene_id, chat_mode, update_time),
    ADD CONSTRAINT fk_ai_conversation_scene FOREIGN KEY (scene_id) REFERENCES ai_scene (id) ON DELETE SET NULL;

INSERT INTO ai_scene (scene_code, scene_name, description, chat_mode, enabled)
VALUES
    ('default_single', '默认单聊场景', '每个 AI 保持独立上下文，适合分别调试不同 AI 助手', 'single', 1),
    ('default_team', '默认团队场景', '多个 AI 共用同一份上下文，适合模拟 AI 团队协作', 'team', 1);

INSERT INTO ai_scene_agent (scene_id, agent_id, role_name, sort_order, enabled)
SELECT scene.id, agent.id, agent.agent_name, agent.id, 1
FROM ai_scene scene
JOIN ai_agent agent ON agent.enabled = 1
WHERE scene.scene_code IN ('default_single', 'default_team');

UPDATE ai_conversation conversation
JOIN ai_scene scene ON scene.scene_code = 'default_single'
SET conversation.scene_id = scene.id,
    conversation.chat_mode = 'single'
WHERE conversation.scene_id IS NULL;
