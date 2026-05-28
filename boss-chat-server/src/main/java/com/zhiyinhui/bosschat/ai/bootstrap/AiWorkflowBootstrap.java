package com.zhiyinhui.bosschat.ai.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiWorkflow;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiWorkflowMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class AiWorkflowBootstrap implements CommandLineRunner {

    private final AiAgentMapper aiAgentMapper;
    private final AiWorkflowMapper aiWorkflowMapper;
    private final boolean demoDataEnabled;

    public AiWorkflowBootstrap(
            AiAgentMapper aiAgentMapper,
            AiWorkflowMapper aiWorkflowMapper,
            @Value("${app.bootstrap.demo-data-enabled:false}") boolean demoDataEnabled
    ) {
        this.aiAgentMapper = aiAgentMapper;
        this.aiWorkflowMapper = aiWorkflowMapper;
        this.demoDataEnabled = demoDataEnabled;
    }

    @Override
    public void run(String... args) {
        if (!demoDataEnabled) {
            return;
        }

        ensureWorkflow(
                "workspace_engineer",
                "coding_task",
                "代码任务工作流",
                "用于读取代码、修改代码、执行验证并总结结果",
                """
                {
                  "steps": [
                    "先理解需求并列出需要检查的文件",
                    "读取相关文件与目录",
                    "完成最小必要修改",
                    "执行测试或构建命令验证",
                    "查看 Git diff 并总结"
                  ]
                }
                """
        );
        ensureWorkflow(
                "business_advisor",
                "consulting_analysis",
                "企业咨询分析流程",
                "用于企业咨询场景下的问题拆解、诊断和行动建议",
                """
                {
                  "steps": [
                    "先确认客户业务背景和核心问题",
                    "拆出现状、目标、约束和风险",
                    "给出可执行的优先级建议",
                    "把建议整理为老板能直接沟通的表达"
                  ]
                }
                """
        );
    }

    private void ensureWorkflow(String agentCode, String code, String name, String description, String definitionJson) {
        AiAgent agent = aiAgentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (agent == null) {
            return;
        }
        AiWorkflow workflow = aiWorkflowMapper.selectOne(new LambdaQueryWrapper<AiWorkflow>()
                .eq(AiWorkflow::getAgentId, agent.getId())
                .eq(AiWorkflow::getWorkflowCode, code)
                .last("LIMIT 1"));
        if (workflow == null) {
            workflow = new AiWorkflow();
            workflow.setAgentId(agent.getId());
            workflow.setWorkflowCode(code);
        }
        workflow.setWorkflowName(name);
        workflow.setDescription(description);
        workflow.setDefinitionJson(definitionJson);
        workflow.setEnabled(1);
        if (workflow.getId() == null) {
            aiWorkflowMapper.insert(workflow);
        } else {
            aiWorkflowMapper.updateById(workflow);
        }
    }
}
