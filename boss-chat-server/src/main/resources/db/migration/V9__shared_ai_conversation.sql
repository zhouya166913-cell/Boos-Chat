ALTER TABLE ai_conversation
    DROP FOREIGN KEY fk_ai_conversation_agent;

ALTER TABLE ai_conversation
    MODIFY COLUMN agent_id BIGINT NULL COMMENT '创建或最近使用的AI ID；V9后会话本身不再强绑定单个AI';

ALTER TABLE ai_conversation
    ADD CONSTRAINT fk_ai_conversation_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE SET NULL;

ALTER TABLE ai_message
    ADD COLUMN agent_id BIGINT NULL COMMENT 'AI消息所属的发言AI；用户消息为空' AFTER user_id,
    ADD COLUMN agent_name VARCHAR(120) NOT NULL DEFAULT '' COMMENT 'AI发言名称快照' AFTER agent_id,
    ADD KEY idx_ai_message_agent (agent_id),
    ADD CONSTRAINT fk_ai_message_agent FOREIGN KEY (agent_id) REFERENCES ai_agent (id) ON DELETE SET NULL;

UPDATE ai_message message
JOIN ai_conversation conversation ON conversation.id = message.conversation_id
JOIN ai_agent agent ON agent.id = conversation.agent_id
SET
    message.agent_id = conversation.agent_id,
    message.agent_name = agent.agent_name
WHERE message.role = 'assistant';
