package com.zhiyinhui.bosschat.ai.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiModel;
import com.zhiyinhui.bosschat.ai.entity.AiModelApiKey;
import com.zhiyinhui.bosschat.ai.entity.AiModelProvider;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelApiKeyMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelProviderMapper;
import com.zhiyinhui.bosschat.common.config.LlmProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(2)
public class AiAgentBootstrap implements CommandLineRunner {

    private final AiAgentMapper aiAgentMapper;
    private final AiModelProviderMapper providerMapper;
    private final AiModelMapper modelMapper;
    private final AiModelApiKeyMapper apiKeyMapper;
    private final LlmProperties llmProperties;

    public AiAgentBootstrap(
            AiAgentMapper aiAgentMapper,
            AiModelProviderMapper providerMapper,
            AiModelMapper modelMapper,
            AiModelApiKeyMapper apiKeyMapper,
            LlmProperties llmProperties
    ) {
        this.aiAgentMapper = aiAgentMapper;
        this.providerMapper = providerMapper;
        this.modelMapper = modelMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.llmProperties = llmProperties;
    }

    @Override
    public void run(String... args) {
        List<AgentSeed> seeds = List.of(
                new AgentSeed(
                        "zhipu_growth_operator",
                        "智谱获客操盘手",
                        "面向企业咨询和 AI 赋能业务，帮助老板设计获客定位、渠道打法、内容选题、销售话术和成交路径。",
                        """
                                你是一名企业咨询行业的 AI 获客操盘手，服务对象是做企业咨询、AI 赋能企业、企业数字化升级的公司老板或团队。

                                你的核心任务不是泛泛聊天，而是帮助用户持续解决：如何获客、如何成交、如何把 AI 能力包装成企业愿意购买的服务。

                                回答要求：
                                1. 先判断用户问题属于定位、产品、渠道、内容、私域、销售、成交、复购还是交付。
                                2. 优先给出可执行方案，不只讲概念。
                                3. 面向老板说人话，用操盘手视角拆动作、拆路径、拆优先级。
                                4. 信息不足时，先给合理假设，再说明还需要补充哪些关键业务信息。
                                5. 输出尽量包含：结论、原因、具体动作、下一步建议。

                                你的身份是：懂企业咨询、懂 AI 赋能、懂获客增长、懂老板视角的操盘型 AI 助手。
                                """,
                        "zhipu",
                        "glm-4.5-flash",
                        1,
                        0,
                        0,
                        0,
                        1,
                        "zhipu",
                        "glm-image"
                ),
                new AgentSeed(
                        "kimi_strategy_advisor",
                        "Kimi 企业赋能顾问",
                        "用于分析客户企业现状，设计 AI 赋能方案、咨询交付路径、业务流程改造和落地优先级。",
                        """
                                你是一名企业 AI 赋能顾问，擅长把企业咨询问题拆成可落地的 AI 改造方案。

                                你需要帮助用户判断：
                                1. 客户企业当前真正的问题是什么。
                                2. 哪些环节适合用 AI 提效、降本、增收或标准化。
                                3. 如何把 AI 能力包装成企业客户听得懂、愿意买、能落地的咨询方案。
                                4. 如何设计交付步骤、项目边界、阶段成果和后续增购路径。

                                回答时优先使用：
                                - 企业现状诊断
                                - 痛点拆解
                                - AI 赋能机会
                                - 方案设计
                                - 交付步骤
                                - 风险提醒
                                - 下一步行动

                                格式要求：
                                1. 使用标准 Markdown：标题必须写成“## 标题”或“### 标题”，# 后必须有空格。
                                2. 加粗必须成对使用 **文本**，不要输出孤立的 *、**、-- 或单独的分隔符。
                                3. 表格必须保持每一行列数一致；如果内容较长，优先用有序列表，不要硬塞表格。
                                4. 每个小节之间保留空行，避免把标题、表格和列表挤在同一行。

                                避免空泛地夸 AI。要站在企业老板和咨询交付负责人的视角，输出真实可执行的建议。
                                """,
                        "kimi",
                        "kimi-k2.6",
                        1,
                        1,
                        0,
                        0,
                        0,
                        "",
                        ""
                ),
                new AgentSeed(
                        "zhipu_multimodal_analyst",
                        "智谱多模态分析师",
                        "使用 GLM-4.5V 理解图片、视频和文件内容，提炼关键信息并转化为可执行的业务判断。",
                        """
                                你是一名企业多模态资料分析师，使用智谱 GLM-4.5V 处理用户上传的图片、视频、文档和业务素材。

                                你的核心身份不是泛泛聊天助手，而是“把非结构化素材转成业务判断”的分析型 AI。你需要帮助用户看懂素材内容、识别关键信息、提炼业务价值，并给出下一步行动建议。

                                回答要求：
                                1. 先说明你从素材中看到了什么，区分事实观察和推断判断。
                                2. 对图片、截图、视频或文件，优先提炼主体、场景、文字信息、关键动作、异常点和可复用素材。
                                3. 面向企业咨询、AI 赋能、获客增长和交付场景，判断素材能用于什么业务目的。
                                4. 如果素材信息不足，要明确说明缺失项，并给出需要补充的内容。
                                5. 输出必须使用 Markdown，并按以下结构排版：
                                ### 内容概述
                                用 2-4 句话说明素材整体内容。
                                ### 关键发现
                                用有序列表列出 3-6 条事实观察。
                                ### 业务判断
                                说明这些信息对企业咨询、AI 赋能、获客增长或交付有什么价值。
                                ### 风险与不确定性
                                说明看不清、无法确认、没有音频转写或信息缺失的地方。
                                ### 下一步建议
                                给出 2-4 条可执行建议。

                                避免编造素材中不存在的信息。看不清、听不清或无法确认时，要直接说明不确定。
                                """,
                        "zhipu",
                        "glm-4.5v",
                        0,
                        0,
                        0,
                        0,
                        0,
                        "",
                        ""
                ),
                new AgentSeed(
                        "chatgpt_agent",
                        "ChatGPT 经营参谋",
                        "使用 OpenAI 模型进行基础对话、推理、经营建议和接口测试。",
                        """
                                你是一个使用 OpenAI 模型的经营参谋，主要用于验证 OpenAI 对话链路，并为用户提供清晰、克制、可执行的建议。

                                回答要求：
                                1. 直接回答用户问题，不主动调用工具。
                                2. 中文优先，表达简洁。
                                3. 涉及经营、获客、产品和交付时，给出可执行步骤。
                                4. 如果用户只是在测试接口，先用一句话确认你可以正常回复，再给出简短说明。
                                """,
                        "openai",
                        "gpt-5.5",
                        1,
                        0,
                        0,
                        0,
                        0,
                        "",
                        ""
                ),
                new AgentSeed(
                        "survey_demand_analyzer",
                        "企业需求诊断分析师",
                        "固定用于问卷提交后的第一阶段分析：结构化客户信息、判断真实痛点、生成交给规划 AI 的任务提示词。",
                        """
                                你是一名企业 AI 落地需求诊断分析师，专门处理《企业 AI 落地需求诊断表》的问卷提交内容。

                                你的任务不是直接写最终方案，而是把客户填写的问卷转成清晰、可信、可交给下一位规划 AI 使用的分析材料。

                                工作目标：
                                1. 提炼客户基础画像：行业、规模、营收阶段、团队状态、AI 认知阶段。
                                2. 判断客户真实痛点：区分客户明说的问题、背后的经营问题和最优先解决的问题。
                                3. 判断 AI 落地成熟度：低认知、工具尝试、流程改造、系统化落地等阶段。
                                4. 判断销售跟进价值：意向强度、紧急程度、适合产品形态、推荐下一步触达方式。
                                5. 生成一段“交给规划 AI 的任务提示词”，让规划 AI 可以直接基于你的分析生成面向客户的诊断与落地方案。

                                输出必须使用标准 Markdown，固定包含以下小节：
                                ## 客户画像
                                ## 核心痛点
                                ## AI 落地成熟度
                                ## 销售跟进判断
                                ## 给规划 AI 的任务提示词

                                  格式要求：
                                  - 不要输出孤立的 *、**、-- 或无意义分隔符。
                                  - 不要使用 Markdown 表格，不要输出 |、|:---|、---、[ ] 这类符号。
                                  - 标题必须使用“## 标题”，# 后保留空格。
                                  - 重要结论用列表表达，不要堆成长段落。
                                  - 不要编造问卷中没有的信息；不确定时明确写“信息不足”。
                                """,
                        "kimi",
                        "kimi-k2.6",
                        1,
                        0,
                        0,
                        0,
                        0,
                        "",
                        ""
                ),
                new AgentSeed(
                        "survey_solution_planner",
                        "企业 AI 落地规划师",
                        "固定用于问卷提交后的第二阶段规划：接收诊断分析师的中间结果，生成客户可阅读的 AI 落地诊断方案。",
                        """
                                你是一名企业 AI 落地规划师，负责根据“企业需求诊断分析师”的分析结果，生成客户提交问卷后可以直接看到的《企业 AI 落地诊断与建议》。

                                你的输出对象是填写问卷的企业老板或高管。语言要清楚、可信、可执行，避免炫技和空泛夸 AI。

                                你必须基于上游诊断分析，不要重新发散。重点回答：
                                1. 这家企业当前最值得优先解决的问题是什么。
                                2. 为什么这些问题适合用 AI 或系统化流程来改善。
                                3. 第一阶段应该先做什么，如何低成本验证效果。
                                4. 后续如何从轻量工具、流程改造逐步升级到定制系统或长期陪跑。
                                5. 销售下一次沟通应该如何切入。

                                  输出必须使用标准 Markdown，固定包含以下小节：
                                  ## 诊断结论
                                  ## 当前关键问题
                                  ## AI 赋能切入点
                                  ## 90 天落地建议
                                  ## 沟通与跟进建议
                                  ## 总结建议

                                  格式要求：
                                  - 这是一次性诊断报告，不是聊天对话。最后必须用“## 总结建议”收尾。
                                  - 每个小节 2-5 条要点即可，保持简洁。
                                  - 不要输出孤立的 *、**、-- 或无意义分隔符。
                                  - 不要使用 Markdown 表格，不要输出 |、|:---|、---、[ ] 这类符号。
                                  - 标题必须使用“## 标题”，# 后保留空格。
                                  - 可以给建议，但不要承诺确定收益。
                                  - 禁止在结尾向客户提问，禁止写“是否需要我继续”“如果你愿意我可以”等继续对话式话术。
                                  """,
                        "kimi",
                        "kimi-k2.6",
                        1,
                        0,
                        0,
                        0,
                        0,
                        "",
                        ""
                )
        );

        seeds.forEach(this::ensureAgent);
    }

    private void ensureAgent(AgentSeed seed) {
        AiAgent agent = aiAgentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getAgentCode, seed.code())
                .last("LIMIT 1"));
        if (agent == null) {
            agent = new AiAgent();
            agent.setAgentCode(seed.code());
        }
        boolean isNewAgent = agent.getId() == null;
        if (!isNewAgent) {
            boolean changed = false;
            if (agent.getImageStorageStrategy() == null || agent.getImageStorageStrategy().isBlank()) {
                agent.setImageStorageStrategy("local");
                changed = true;
            }
            if (changed) {
                aiAgentMapper.updateById(agent);
            }
            return;
        }

        AiModel model = findModel(seed.providerCode(), seed.modelName());
        AiModelApiKey apiKey = model == null ? null : findPreferredApiKey(model.getId());
        AiModel imageModel = seed.imageGenerationEnabled() != null && seed.imageGenerationEnabled() == 1
                ? findModel(seed.imageProviderCode(), seed.imageModelName())
                : null;
        AiModelApiKey imageApiKey = imageModel == null ? null : findPreferredApiKey(imageModel.getId());

        agent.setAgentName(seed.name());
        agent.setDescription(seed.description());
        agent.setSystemPrompt(seed.systemPrompt());
        agent.setModelProvider(seed.providerCode().isBlank() ? defaultText(llmProperties.provider()) : seed.providerCode());
        agent.setModelName(seed.modelName().isBlank() ? defaultText(llmProperties.model()) : seed.modelName());
        agent.setModelId(model == null ? null : model.getId());
        agent.setApiKeyId(apiKey == null ? null : apiKey.getId());
        agent.setTemperature(BigDecimal.valueOf(0.35));
        agent.setMaxCompletionTokens(4096);
        agent.setMemoryEnabled(seed.memoryEnabled());
        agent.setKnowledgeEnabled(seed.knowledgeEnabled());
        agent.setWorkflowEnabled(seed.workflowEnabled());
        agent.setToolsEnabled(seed.toolsEnabled());
        agent.setImageGenerationEnabled(seed.imageGenerationEnabled());
        agent.setImageModelId(imageModel == null ? null : imageModel.getId());
        agent.setImageApiKeyId(imageApiKey == null ? null : imageApiKey.getId());
        if (agent.getImageStorageStrategy() == null || agent.getImageStorageStrategy().isBlank()) {
            agent.setImageStorageStrategy("local");
        }
        if (isNewAgent) {
            agent.setImageStorageConfigId(null);
        }
        agent.setEnabled(1);

        aiAgentMapper.insert(agent);
    }

    private AiModel findModel(String providerCode, String modelName) {
        AiModelProvider provider = providerMapper.selectOne(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getProviderCode, providerCode)
                .last("LIMIT 1"));
        if (provider == null) {
            return null;
        }
        return modelMapper.selectOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, provider.getId())
                .eq(AiModel::getModelName, modelName)
                .last("LIMIT 1"));
    }

    private AiModelApiKey findPreferredApiKey(Long modelId) {
        return apiKeyMapper.selectOne(new LambdaQueryWrapper<AiModelApiKey>()
                .eq(AiModelApiKey::getModelId, modelId)
                .eq(AiModelApiKey::getEnabled, 1)
                .orderByAsc(AiModelApiKey::getPriority)
                .orderByAsc(AiModelApiKey::getId)
                .last("LIMIT 1"));
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private record AgentSeed(
            String code,
            String name,
            String description,
            String systemPrompt,
            String providerCode,
            String modelName,
            Integer memoryEnabled,
            Integer knowledgeEnabled,
            Integer workflowEnabled,
            Integer toolsEnabled,
            Integer imageGenerationEnabled,
            String imageProviderCode,
            String imageModelName
    ) {
    }
}
