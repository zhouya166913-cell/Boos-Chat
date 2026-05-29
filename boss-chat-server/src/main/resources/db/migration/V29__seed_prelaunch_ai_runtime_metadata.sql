-- Pre-launch runtime metadata seed.
-- This script intentionally contains only public configuration metadata.
-- Real API keys and object-storage secrets are kept in private/seed-local-tested-secrets.sql.

INSERT INTO ai_model_provider (
    provider_code,
    provider_name,
    base_url,
    auth_type,
    enabled,
    remark
) VALUES
    (
        'zhipu',
        '智谱 AI',
        'https://docs.bigmodel.cn/cn/guide/develop/openai/introduction',
        'bearer',
        1,
        '智谱 AI 官方文档地址；实际调用接口在模型配置中填写完整 URL。'
    ),
    (
        'kimi',
        'Kimi / Moonshot',
        'https://platform.kimi.com/docs/guide/kimi-k2-5-quickstart',
        'bearer',
        1,
        'Kimi / Moonshot 官方文档地址；实际调用接口在模型配置中填写完整 URL。'
    )
ON DUPLICATE KEY UPDATE
    provider_name = VALUES(provider_name),
    base_url = VALUES(base_url),
    auth_type = VALUES(auth_type),
    enabled = VALUES(enabled),
    remark = VALUES(remark);

INSERT INTO ai_model (
    provider_id,
    model_name,
    display_name,
    model_type,
    api_path,
    billing_type,
    official_doc_url,
    compatibility_profile,
    context_window,
    supports_stream,
    supports_tools,
    supports_vision,
    enabled,
    remark
)
SELECT
    provider.id,
    seed.model_name,
    seed.display_name,
    seed.model_type,
    seed.api_path,
    seed.billing_type,
    seed.official_doc_url,
    seed.compatibility_profile,
    seed.context_window,
    seed.supports_stream,
    seed.supports_tools,
    seed.supports_vision,
    seed.enabled,
    seed.remark
FROM (
    SELECT 'zhipu' AS provider_code, 'glm-5.1' AS model_name, '智谱 GLM-5.1 旗舰大模型' AS display_name, 'chat' AS model_type, 'https://open.bigmodel.cn/api/paas/v4/chat/completions' AS api_path, 'paid' AS billing_type, 'https://docs.bigmodel.cn/cn/guide/models/text/glm-5.1' AS official_doc_url, '' AS compatibility_profile, 200000 AS context_window, 1 AS supports_stream, 1 AS supports_tools, 0 AS supports_vision, 1 AS enabled, '旗舰大模型，适合复杂推理、代码和 Agent 基座。' AS remark
    UNION ALL
    SELECT 'zhipu', 'glm-5v-turbo', '智谱 GLM-5V-Turbo 多模态', 'chat', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 'paid', 'https://docs.bigmodel.cn/cn/guide/models/vlm/glm-5v-turbo', '', 200000, 1, 1, 1, 1, '多模态模型，支持图片、视频和文件理解。'
    UNION ALL
    SELECT 'zhipu', 'glm-5-turbo', '智谱 GLM-5-Turbo Agent/高效模型', 'chat', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 'paid', 'https://docs.bigmodel.cn/cn/guide/models/text/glm-5-turbo', '', 128000, 1, 1, 0, 1, '高效 Agent 和工具调用模型。'
    UNION ALL
    SELECT 'zhipu', 'glm-image', '智谱 GLM-Image 图片生成', 'image_generation', 'https://open.bigmodel.cn/api/paas/v4/images/generations', 'paid', 'https://docs.bigmodel.cn/cn/guide/models/image-generation/glm-image', '', 0, 0, 0, 0, 1, '图片生成模型。'
    UNION ALL
    SELECT 'kimi', 'kimi-k2.6', 'Kimi K2.6 最新旗舰', 'chat', 'https://api.moonshot.cn/v1/chat/completions', 'paid', 'https://platform.kimi.com/', 'kimi_k2', 256000, 1, 1, 0, 1, 'Kimi 最新旗舰，优先用于复杂推理、代码和 Agent 场景；按 K2 兼容规则调用。'
    UNION ALL
    SELECT 'kimi', 'kimi-k2.5', 'Kimi K2.5 多模态', 'chat', 'https://api.moonshot.cn/v1/chat/completions', 'paid', 'https://platform.kimi.com/docs/guide/kimi-k2-5-quickstart', 'kimi_k2', 256000, 1, 1, 1, 1, '多模态模型，支持视觉与文本输入。'
) seed
JOIN ai_model_provider provider ON provider.provider_code = seed.provider_code
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    model_type = VALUES(model_type),
    api_path = VALUES(api_path),
    billing_type = VALUES(billing_type),
    official_doc_url = VALUES(official_doc_url),
    compatibility_profile = VALUES(compatibility_profile),
    context_window = VALUES(context_window),
    supports_stream = VALUES(supports_stream),
    supports_tools = VALUES(supports_tools),
    supports_vision = VALUES(supports_vision),
    enabled = VALUES(enabled),
    remark = VALUES(remark);

INSERT INTO ai_agent (
    agent_code,
    agent_name,
    description,
    system_prompt,
    model_provider,
    model_name,
    model_id,
    api_key_id,
    temperature,
    max_completion_tokens,
    memory_enabled,
    knowledge_enabled,
    workflow_enabled,
    tools_enabled,
    image_generation_enabled,
    image_model_id,
    image_api_key_id,
    image_storage_strategy,
    image_storage_config_id,
    enabled
)
SELECT
    'survey_demand_analyzer',
    '企业需求诊断分析师',
    '固定用于问卷提交后的第一阶段：分析客户画像、核心痛点和销售跟进价值。',
    CONCAT(
        '你是企业 AI 落地需求诊断分析师，专门处理《企业 AI 落地需求诊断表》的问卷和业务调研内容。', CHAR(10), CHAR(10),
        '你的任务不是直接写最终方案，而是把客户填写的内容转成清晰、可信、可交给下一位规划 AI 使用的分析材料。', CHAR(10), CHAR(10),
        '请重点分析：', CHAR(10),
        '1. 客户画像：行业、主营产品或服务、客户群体、团队规模、营收阶段、AI 认知阶段。', CHAR(10),
        '2. 真实痛点：区分客户明说的问题、背后的经营问题和最优先解决的问题。', CHAR(10),
        '3. AI 落地成熟度：低认知、工具尝试、流程改造、系统化落地等阶段。', CHAR(10),
        '4. 销售跟进价值：意向强度、紧急程度、适合产品形态、推荐下一步触达方式。', CHAR(10),
        '5. 给规划 AI 的任务提示词：让规划 AI 可以基于你的分析生成面向客户的诊断与落地方案。', CHAR(10), CHAR(10),
        '输出要求：', CHAR(10),
        '标题直接使用中文标题，例如“客户画像”“核心痛点”“AI 落地成熟度”“销售跟进判断”“给规划 AI 的任务提示词”。', CHAR(10),
        '禁止使用 Markdown 表格，禁止输出 #、##、---、|、[ ] 这类容易破坏页面排版的符号。', CHAR(10),
        '每个标题下面使用 2 到 5 条短句，优先用“判断：”“依据：”“建议：”开头。', CHAR(10),
        '不要编造问卷中没有的信息；不确定时明确写“信息不足”。'
    ),
    'kimi',
    'kimi-k2.6',
    (SELECT model.id FROM ai_model model JOIN ai_model_provider provider ON provider.id = model.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' LIMIT 1),
    (SELECT api_key.id FROM ai_model_api_key api_key JOIN ai_model model ON model.id = api_key.model_id JOIN ai_model_provider provider ON provider.id = api_key.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' AND api_key.enabled = 1 ORDER BY api_key.priority ASC, api_key.id ASC LIMIT 1),
    0.25,
    8192,
    0,
    0,
    0,
    0,
    0,
    NULL,
    NULL,
    'local',
    NULL,
    1
ON DUPLICATE KEY UPDATE
    agent_name = VALUES(agent_name),
    description = VALUES(description),
    system_prompt = VALUES(system_prompt),
    model_provider = VALUES(model_provider),
    model_name = VALUES(model_name),
    model_id = VALUES(model_id),
    api_key_id = COALESCE(VALUES(api_key_id), api_key_id),
    temperature = VALUES(temperature),
    max_completion_tokens = VALUES(max_completion_tokens),
    memory_enabled = VALUES(memory_enabled),
    knowledge_enabled = VALUES(knowledge_enabled),
    workflow_enabled = VALUES(workflow_enabled),
    tools_enabled = VALUES(tools_enabled),
    image_generation_enabled = VALUES(image_generation_enabled),
    image_model_id = VALUES(image_model_id),
    image_api_key_id = VALUES(image_api_key_id),
    image_storage_strategy = VALUES(image_storage_strategy),
    image_storage_config_id = VALUES(image_storage_config_id),
    enabled = VALUES(enabled);

INSERT INTO ai_agent (
    agent_code,
    agent_name,
    description,
    system_prompt,
    model_provider,
    model_name,
    model_id,
    api_key_id,
    temperature,
    max_completion_tokens,
    memory_enabled,
    knowledge_enabled,
    workflow_enabled,
    tools_enabled,
    image_generation_enabled,
    image_model_id,
    image_api_key_id,
    image_storage_strategy,
    image_storage_config_id,
    enabled
)
SELECT
    'survey_solution_planner',
    '企业 AI 落地规划师',
    '固定用于问卷提交后的第二阶段：生成客户可直接查看的诊断和落地建议。',
    CONCAT(
        '你是企业 AI 落地规划师，负责根据“企业需求诊断分析师”的分析结果，生成客户提交问卷后可以直接看到的《企业 AI 落地诊断与建议》。', CHAR(10), CHAR(10),
        '输出对象是填写问卷的企业老板或高管。语言要清晰、可信、可执行，避免炫技和空泛夸 AI。', CHAR(10), CHAR(10),
        '这是一次性诊断报告，不是聊天对话。请直接给出结论、解决方案和落地路径，最后用“总结建议”自然收尾。', CHAR(10),
        '必须重点结合客户的主营产品或服务、主要客户群体、获客渠道、业务流程或系统使用情况做行业化判断。', CHAR(10), CHAR(10),
        '固定输出以下中文标题：', CHAR(10),
        '诊断结论', CHAR(10),
        '当前关键问题', CHAR(10),
        'AI 赋能切入点', CHAR(10),
        '90 天落地建议', CHAR(10),
        '沟通与跟进建议', CHAR(10),
        '总结建议', CHAR(10), CHAR(10),
        '格式要求：', CHAR(10),
        '标题前不要加任何符号，禁止使用 #、##、---、|、[ ] 和 Markdown 表格。', CHAR(10),
        '每个标题下面优先使用 1. 2. 3. 这样的数字序号组织内容，每条只表达一个重点。', CHAR(10),
        '段落要短，重点要前置，让客户一眼能看到结论。', CHAR(10), CHAR(10),
        '重点标记要求：', CHAR(10),
        '只允许使用以下三种标记包裹真正重要的文字：', CHAR(10),
        '【痛点：客户最关键的经营痛点】', CHAR(10),
        '【需求：客户最明确的业务需求】', CHAR(10),
        '【关键方案：最值得优先执行的解决方案】', CHAR(10),
        '每个小节最多标记 1 到 2 处重点，不要把普通标签、普通原因或整段话全部标记。', CHAR(10), CHAR(10),
        '可以给建议，但不要承诺确定收益。', CHAR(10),
        '禁止在结尾向客户提问，禁止写“是否需要我继续”“如果你愿意我可以”等继续对话式话术。'
    ),
    'kimi',
    'kimi-k2.6',
    (SELECT model.id FROM ai_model model JOIN ai_model_provider provider ON provider.id = model.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' LIMIT 1),
    (SELECT api_key.id FROM ai_model_api_key api_key JOIN ai_model model ON model.id = api_key.model_id JOIN ai_model_provider provider ON provider.id = api_key.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' AND api_key.enabled = 1 ORDER BY api_key.priority ASC, api_key.id ASC LIMIT 1),
    0.35,
    8192,
    0,
    0,
    0,
    0,
    0,
    NULL,
    NULL,
    'local',
    NULL,
    1
ON DUPLICATE KEY UPDATE
    agent_name = VALUES(agent_name),
    description = VALUES(description),
    system_prompt = VALUES(system_prompt),
    model_provider = VALUES(model_provider),
    model_name = VALUES(model_name),
    model_id = VALUES(model_id),
    api_key_id = COALESCE(VALUES(api_key_id), api_key_id),
    temperature = VALUES(temperature),
    max_completion_tokens = VALUES(max_completion_tokens),
    memory_enabled = VALUES(memory_enabled),
    knowledge_enabled = VALUES(knowledge_enabled),
    workflow_enabled = VALUES(workflow_enabled),
    tools_enabled = VALUES(tools_enabled),
    image_generation_enabled = VALUES(image_generation_enabled),
    image_model_id = VALUES(image_model_id),
    image_api_key_id = VALUES(image_api_key_id),
    image_storage_strategy = VALUES(image_storage_strategy),
    image_storage_config_id = VALUES(image_storage_config_id),
    enabled = VALUES(enabled);

INSERT INTO ai_agent (
    agent_code,
    agent_name,
    description,
    system_prompt,
    model_provider,
    model_name,
    model_id,
    api_key_id,
    temperature,
    max_completion_tokens,
    memory_enabled,
    knowledge_enabled,
    workflow_enabled,
    tools_enabled,
    image_generation_enabled,
    image_model_id,
    image_api_key_id,
    image_storage_strategy,
    image_storage_config_id,
    enabled
)
SELECT
    'ai_operation_growth_operator',
    'AI运营操盘手',
    '面向企业老板和管理层，拆解经营目标、流程效率、团队协同与 AI 落地节奏。',
    CONCAT(
        '你是 AI 运营操盘手，服务对象是企业老板、业务负责人和运营管理者。', CHAR(10), CHAR(10),
        '你的核心职责是把“想增长、想提效、想落地 AI”的模糊目标拆成可执行的经营动作。', CHAR(10), CHAR(10),
        '工作方式：', CHAR(10),
        '1. 先识别企业当前阶段：获客、转化、交付、复购、组织协同、数据化管理分别卡在哪里。', CHAR(10),
        '2. 再判断优先级：只抓最影响收入、效率和团队执行的关键问题。', CHAR(10),
        '3. 输出方案时要给出节奏：先做什么、谁负责、用什么工具、如何衡量结果。', CHAR(10),
        '4. 涉及 AI 时，只讲能落地的场景，例如销售跟进、客户分层、内容生产、客服质检、数据看板、流程自动化。', CHAR(10),
        '5. 不空泛鼓励，不承诺确定收益；信息不足时先列出需要补齐的信息。', CHAR(10), CHAR(10),
        '输出风格：', CHAR(10),
        '语言直接、像操盘顾问；多给清单、步骤、判断依据。', CHAR(10),
        '默认使用“诊断判断”“优先事项”“执行方案”“检查指标”“下一步动作”组织回答。'
    ),
    'kimi',
    'kimi-k2.6',
    (SELECT model.id FROM ai_model model JOIN ai_model_provider provider ON provider.id = model.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' LIMIT 1),
    (SELECT api_key.id FROM ai_model_api_key api_key JOIN ai_model model ON model.id = api_key.model_id JOIN ai_model_provider provider ON provider.id = api_key.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' AND api_key.enabled = 1 ORDER BY api_key.priority ASC, api_key.id ASC LIMIT 1),
    0.35,
    8192,
    1,
    1,
    1,
    1,
    0,
    NULL,
    NULL,
    'local',
    NULL,
    1
ON DUPLICATE KEY UPDATE
    agent_name = VALUES(agent_name),
    description = VALUES(description),
    system_prompt = VALUES(system_prompt),
    model_provider = VALUES(model_provider),
    model_name = VALUES(model_name),
    model_id = VALUES(model_id),
    api_key_id = COALESCE(VALUES(api_key_id), api_key_id),
    temperature = VALUES(temperature),
    max_completion_tokens = VALUES(max_completion_tokens),
    memory_enabled = VALUES(memory_enabled),
    knowledge_enabled = VALUES(knowledge_enabled),
    workflow_enabled = VALUES(workflow_enabled),
    tools_enabled = VALUES(tools_enabled),
    image_generation_enabled = VALUES(image_generation_enabled),
    image_model_id = VALUES(image_model_id),
    image_api_key_id = VALUES(image_api_key_id),
    image_storage_strategy = VALUES(image_storage_strategy),
    image_storage_config_id = VALUES(image_storage_config_id),
    enabled = VALUES(enabled);

INSERT INTO ai_agent (
    agent_code,
    agent_name,
    description,
    system_prompt,
    model_provider,
    model_name,
    model_id,
    api_key_id,
    temperature,
    max_completion_tokens,
    memory_enabled,
    knowledge_enabled,
    workflow_enabled,
    tools_enabled,
    image_generation_enabled,
    image_model_id,
    image_api_key_id,
    image_storage_strategy,
    image_storage_config_id,
    enabled
)
SELECT
    'ai_customer_acquisition_master',
    'AI获客大师',
    '围绕定位、渠道、内容、线索转化和私域承接，生成可执行的获客方案和营销素材。',
    CONCAT(
        '你是 AI 获客大师，服务对象是需要拿到线索、提升转化、扩大曝光的企业老板和销售团队。', CHAR(10), CHAR(10),
        '你的核心目标不是写漂亮文案，而是帮助企业形成可执行、可验证、可复用的获客动作。', CHAR(10), CHAR(10),
        '工作方式：', CHAR(10),
        '1. 先判断产品、客群、客单价、成交周期和客户决策链。', CHAR(10),
        '2. 再选择获客路径：短视频、直播、私域、社群、搜索、朋友圈、地推、渠道合作、老客转介绍等。', CHAR(10),
        '3. 输出内容时要贴近真实销售场景，包括钩子、痛点、利益点、信任背书、行动引导。', CHAR(10),
        '4. 需要时可以生成销售话术、私信脚本、朋友圈内容、短视频脚本、社群活动方案和客户跟进 SOP。', CHAR(10),
        '5. 如果用户要求图片或海报创意，要先给出清晰提示词，再调用图片生成能力。', CHAR(10), CHAR(10),
        '输出风格：', CHAR(10),
        '直接给方案，少讲概念；每次回答尽量包含“目标客户”“核心卖点”“获客动作”“内容样例”“转化跟进”。'
    ),
    'kimi',
    'kimi-k2.6',
    (SELECT model.id FROM ai_model model JOIN ai_model_provider provider ON provider.id = model.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' LIMIT 1),
    (SELECT api_key.id FROM ai_model_api_key api_key JOIN ai_model model ON model.id = api_key.model_id JOIN ai_model_provider provider ON provider.id = api_key.provider_id WHERE provider.provider_code = 'kimi' AND model.model_name = 'kimi-k2.6' AND api_key.enabled = 1 ORDER BY api_key.priority ASC, api_key.id ASC LIMIT 1),
    0.55,
    8192,
    1,
    1,
    1,
    1,
    1,
    (SELECT model.id FROM ai_model model JOIN ai_model_provider provider ON provider.id = model.provider_id WHERE provider.provider_code = 'zhipu' AND model.model_name = 'glm-image' LIMIT 1),
    (SELECT api_key.id FROM ai_model_api_key api_key JOIN ai_model model ON model.id = api_key.model_id JOIN ai_model_provider provider ON provider.id = api_key.provider_id WHERE provider.provider_code = 'zhipu' AND model.model_name = 'glm-image' AND api_key.enabled = 1 ORDER BY api_key.priority ASC, api_key.id ASC LIMIT 1),
    'cos',
    (SELECT id FROM ai_image_storage_config WHERE storage_code = 'tencent_cos_lantu_1308986692_ap_nanjing' AND enabled = 1 LIMIT 1),
    1
ON DUPLICATE KEY UPDATE
    agent_name = VALUES(agent_name),
    description = VALUES(description),
    system_prompt = VALUES(system_prompt),
    model_provider = VALUES(model_provider),
    model_name = VALUES(model_name),
    model_id = VALUES(model_id),
    api_key_id = COALESCE(VALUES(api_key_id), api_key_id),
    temperature = VALUES(temperature),
    max_completion_tokens = VALUES(max_completion_tokens),
    memory_enabled = VALUES(memory_enabled),
    knowledge_enabled = VALUES(knowledge_enabled),
    workflow_enabled = VALUES(workflow_enabled),
    tools_enabled = VALUES(tools_enabled),
    image_generation_enabled = VALUES(image_generation_enabled),
    image_model_id = VALUES(image_model_id),
    image_api_key_id = COALESCE(VALUES(image_api_key_id), image_api_key_id),
    image_storage_strategy = VALUES(image_storage_strategy),
    image_storage_config_id = COALESCE(VALUES(image_storage_config_id), image_storage_config_id),
    enabled = VALUES(enabled);

INSERT INTO ai_scene (
    scene_code,
    scene_name,
    description,
    chat_mode,
    enabled
) VALUES (
    'one_on_one_guidance',
    '1v1指导',
    '每个 AI 独立上下文，用于企业运营、获客增长和日常咨询。',
    'single',
    1
)
ON DUPLICATE KEY UPDATE
    scene_name = VALUES(scene_name),
    description = VALUES(description),
    chat_mode = VALUES(chat_mode),
    enabled = VALUES(enabled);

INSERT INTO ai_scene_agent (
    scene_id,
    agent_id,
    role_name,
    sort_order,
    enabled
)
SELECT scene.id, agent.id, agent.agent_name, seed.sort_order, 1
FROM ai_scene scene
JOIN (
    SELECT 'ai_operation_growth_operator' AS agent_code, 10 AS sort_order
    UNION ALL
    SELECT 'ai_customer_acquisition_master', 20
) seed ON 1 = 1
JOIN ai_agent agent ON agent.agent_code = seed.agent_code
WHERE scene.scene_code = 'one_on_one_guidance'
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled);
