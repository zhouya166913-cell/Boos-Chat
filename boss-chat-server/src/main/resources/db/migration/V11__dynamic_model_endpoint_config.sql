ALTER TABLE ai_model
    ADD COLUMN api_path VARCHAR(255) NOT NULL DEFAULT '' COMMENT '模型调用接口路径，例如 /chat/completions、/images/generations、/zrag/retrieval/retrieve' AFTER model_type,
    ADD COLUMN billing_type VARCHAR(40) NOT NULL DEFAULT 'unknown' COMMENT '计费类型：free/paid/unknown' AFTER api_path,
    ADD COLUMN official_doc_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '官方 API 文档地址' AFTER billing_type;

UPDATE ai_model
SET api_path = CASE
    WHEN model_type = 'image_generation' THEN '/images/generations'
    ELSE '/chat/completions'
END
WHERE api_path = '';

UPDATE ai_model
SET billing_type = 'paid',
    official_doc_url = 'https://docs.bigmodel.cn/cn/guide/models/text/glm-4.7'
WHERE model_name = 'glm-4.7';

UPDATE ai_model
SET billing_type = 'free',
    official_doc_url = 'https://docs.bigmodel.cn/cn/guide/models/free/glm-4.5-flash'
WHERE model_name = 'glm-4.5-flash';

UPDATE ai_model
SET billing_type = 'paid',
    official_doc_url = 'https://docs.bigmodel.cn/cn/guide/models/image-generation/glm-image'
WHERE model_name = 'glm-image';

UPDATE ai_model
SET billing_type = 'paid',
    official_doc_url = 'https://platform.kimi.com/docs/api/overview'
WHERE model_name = 'kimi-k2.6';
