package com.zhiyinhui.bosschat.ai.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiScene;
import com.zhiyinhui.bosschat.ai.entity.AiSceneAgent;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiSceneAgentMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiSceneMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(3)
public class AiSceneBootstrap implements CommandLineRunner {

    private static final String DEFAULT_SCENE_CODE = "default_single";
    private static final String SURVEY_DIAGNOSIS_SCENE_CODE = "enterprise_demand_diagnosis";

    private final AiSceneMapper sceneMapper;
    private final AiSceneAgentMapper sceneAgentMapper;
    private final AiAgentMapper agentMapper;
    private final boolean demoDataEnabled;

    public AiSceneBootstrap(
            AiSceneMapper sceneMapper,
            AiSceneAgentMapper sceneAgentMapper,
            AiAgentMapper agentMapper,
            @Value("${app.bootstrap.demo-data-enabled:false}") boolean demoDataEnabled
    ) {
        this.sceneMapper = sceneMapper;
        this.sceneAgentMapper = sceneAgentMapper;
        this.agentMapper = agentMapper;
        this.demoDataEnabled = demoDataEnabled;
    }

    @Override
    public void run(String... args) {
        if (!demoDataEnabled) {
            return;
        }

        AiScene scene = ensureDefaultScene();
        bindAgents(scene, List.of(
                "zhipu_growth_operator",
                "kimi_strategy_advisor",
                "zhipu_multimodal_analyst",
                "chatgpt_agent"
        ));
        AiScene surveyScene = ensureSurveyDiagnosisScene();
        bindAgents(surveyScene, List.of(
                "survey_demand_analyzer",
                "survey_solution_planner"
        ));
    }

    private AiScene ensureDefaultScene() {
        AiScene scene = sceneMapper.selectOne(new LambdaQueryWrapper<AiScene>()
                .eq(AiScene::getSceneCode, DEFAULT_SCENE_CODE)
                .last("LIMIT 1"));
        if (scene == null) {
            scene = new AiScene();
            scene.setSceneCode(DEFAULT_SCENE_CODE);
        }
        scene.setSceneName("默认单聊场景");
        scene.setDescription("测试版默认场景。每个 AI 使用独立上下文，适合分别测试智谱、Kimi 和 OpenAI。");
        scene.setChatMode("single");
        scene.setEnabled(1);
        if (scene.getId() == null) {
            sceneMapper.insert(scene);
            return sceneMapper.selectById(scene.getId());
        }
        sceneMapper.updateById(scene);
        return scene;
    }

    private AiScene ensureSurveyDiagnosisScene() {
        AiScene scene = sceneMapper.selectOne(new LambdaQueryWrapper<AiScene>()
                .eq(AiScene::getSceneCode, SURVEY_DIAGNOSIS_SCENE_CODE)
                .last("LIMIT 1"));
        if (scene == null) {
            scene = new AiScene();
            scene.setSceneCode(SURVEY_DIAGNOSIS_SCENE_CODE);
        }
        scene.setSceneName("企业需求诊断");
        scene.setDescription("固定问卷诊断场景。第一位 Kimi 负责分析问卷并生成规划提示词，第二位 Kimi 负责生成客户可阅读的 AI 落地方案。");
        scene.setChatMode("team");
        scene.setEnabled(1);
        if (scene.getId() == null) {
            sceneMapper.insert(scene);
            return sceneMapper.selectById(scene.getId());
        }
        sceneMapper.updateById(scene);
        return scene;
    }

    private void bindAgents(AiScene scene, List<String> agentCodes) {
        int index = 1;
        for (String agentCode : agentCodes) {
            AiAgent agent = agentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                    .eq(AiAgent::getAgentCode, agentCode)
                    .last("LIMIT 1"));
            if (agent == null) {
                continue;
            }
            AiSceneAgent relation = sceneAgentMapper.selectOne(new LambdaQueryWrapper<AiSceneAgent>()
                    .eq(AiSceneAgent::getSceneId, scene.getId())
                    .eq(AiSceneAgent::getAgentId, agent.getId())
                    .last("LIMIT 1"));
            if (relation == null) {
                relation = new AiSceneAgent();
                relation.setSceneId(scene.getId());
                relation.setAgentId(agent.getId());
            }
            relation.setRoleName(agent.getAgentName());
            relation.setSortOrder(index * 10);
            relation.setEnabled(1);
            if (relation.getId() == null) {
                sceneAgentMapper.insert(relation);
            } else {
                sceneAgentMapper.updateById(relation);
            }
            index++;
        }
    }
}
