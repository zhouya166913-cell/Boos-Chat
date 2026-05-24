-- Rebind OpenAI GPT-5.5 test agents to the free Zhipu GLM-4.5-Flash model.
-- This keeps previously created scenes/conversations pointing at the same agent id,
-- while new requests use the free model for local testing.
UPDATE ai_agent agent
LEFT JOIN ai_model old_model ON old_model.id = agent.model_id
LEFT JOIN ai_model_provider old_provider ON old_provider.id = old_model.provider_id
JOIN ai_model_provider free_provider ON free_provider.provider_code = 'zhipu'
JOIN ai_model free_model ON free_model.provider_id = free_provider.id
                        AND free_model.model_name = 'glm-4.5-flash'
LEFT JOIN ai_model_api_key free_key ON free_key.id = (
    SELECT preferred_key.id
    FROM ai_model_api_key preferred_key
    WHERE preferred_key.model_id = free_model.id
      AND preferred_key.enabled = 1
    ORDER BY preferred_key.priority ASC, preferred_key.id ASC
    LIMIT 1
)
SET agent.model_provider = 'zhipu',
    agent.model_name = 'glm-4.5-flash',
    agent.model_id = free_model.id,
    agent.api_key_id = free_key.id,
    agent.description = CASE
        WHEN agent.agent_name = 'ChatGPT' THEN 'Uses GLM-4.5-Flash free model for local testing.'
        ELSE agent.description
    END,
    agent.agent_name = CASE
        WHEN agent.agent_name = 'ChatGPT' THEN 'Free Test Assistant'
        ELSE agent.agent_name
    END
WHERE (
        old_provider.provider_code = 'openai'
        AND old_model.model_name = 'gpt-5.5'
    )
    OR (
        agent.model_provider = 'openai'
        AND agent.model_name = 'gpt-5.5'
    );
