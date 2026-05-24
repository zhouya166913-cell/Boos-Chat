ALTER TABLE ai_image_storage_config
    ADD COLUMN extra_config_json MEDIUMTEXT NULL COMMENT '扩展配置 JSON，用于保存不同图床或对象存储的特殊参数' AFTER root_path;
