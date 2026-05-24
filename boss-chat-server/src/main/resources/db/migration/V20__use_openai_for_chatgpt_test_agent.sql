DELETE FROM ai_agent
WHERE agent_code = 'openai_free_test';

UPDATE ai_agent agent
JOIN ai_model_provider provider ON provider.provider_code = 'openai'
JOIN ai_model model ON model.provider_id = provider.id
                   AND model.model_name = 'gpt-5.5'
LEFT JOIN ai_model_api_key api_key ON api_key.id = (
    SELECT preferred_key.id
    FROM ai_model_api_key preferred_key
    WHERE preferred_key.model_id = model.id
      AND preferred_key.enabled = 1
    ORDER BY preferred_key.priority ASC, preferred_key.id ASC
    LIMIT 1
)
SET agent.agent_name = 'ChatGPT 免费测试',
    agent.description = '使用 OpenAI 免费测试模型验证基础对话链路，默认关闭工具调用。',
    agent.model_provider = 'openai',
    agent.model_name = 'gpt-5.5',
    agent.model_id = model.id,
    agent.api_key_id = api_key.id,
    agent.memory_enabled = 1,
    agent.knowledge_enabled = 0,
    agent.workflow_enabled = 0,
    agent.tools_enabled = 0,
    agent.image_generation_enabled = 0,
    agent.enabled = 1
WHERE agent.agent_code = 'chatgpt_agent'
   OR agent.agent_name = '免费测试助手';
