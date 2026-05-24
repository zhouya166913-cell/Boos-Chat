-- Final test reset:
-- Keep login accounts in sys_user, clear all generated/business/config data,
-- then let bootstraps recreate roles, permissions, model providers, models,
-- API keys and default agents from the current application configuration.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM ai_tool_execution;
DELETE FROM ai_generated_image;
DELETE FROM ai_usage_record;
DELETE FROM ai_message;
DELETE FROM ai_conversation;
DELETE FROM ai_scene_agent;
DELETE FROM ai_scene;
DELETE FROM ai_memory;
DELETE FROM ai_knowledge_document;
DELETE FROM ai_workflow;
DELETE FROM ai_agent;
DELETE FROM ai_model_api_key;
DELETE FROM ai_model;
DELETE FROM ai_model_provider;
DELETE FROM ai_image_storage_config;

DELETE FROM sys_login_record;
DELETE FROM sys_oauth_account;
DELETE FROM sys_role_permission;
DELETE FROM sys_user_role;
DELETE FROM sys_permission;
DELETE FROM sys_role;

ALTER TABLE ai_tool_execution AUTO_INCREMENT = 1;
ALTER TABLE ai_generated_image AUTO_INCREMENT = 1;
ALTER TABLE ai_usage_record AUTO_INCREMENT = 1;
ALTER TABLE ai_message AUTO_INCREMENT = 1;
ALTER TABLE ai_conversation AUTO_INCREMENT = 1;
ALTER TABLE ai_scene_agent AUTO_INCREMENT = 1;
ALTER TABLE ai_scene AUTO_INCREMENT = 1;
ALTER TABLE ai_memory AUTO_INCREMENT = 1;
ALTER TABLE ai_knowledge_document AUTO_INCREMENT = 1;
ALTER TABLE ai_workflow AUTO_INCREMENT = 1;
ALTER TABLE ai_agent AUTO_INCREMENT = 1;
ALTER TABLE ai_model_api_key AUTO_INCREMENT = 1;
ALTER TABLE ai_model AUTO_INCREMENT = 1;
ALTER TABLE ai_model_provider AUTO_INCREMENT = 1;
ALTER TABLE ai_image_storage_config AUTO_INCREMENT = 1;

ALTER TABLE sys_login_record AUTO_INCREMENT = 1;
ALTER TABLE sys_oauth_account AUTO_INCREMENT = 1;
ALTER TABLE sys_role AUTO_INCREMENT = 1;
ALTER TABLE sys_permission AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;
