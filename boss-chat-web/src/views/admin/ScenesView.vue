<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createScene, listAdminScenes, updateScene, type ScenePayload } from "../../api/scenes";
import { listAdminAgents, type Agent } from "../../api/agents";
import type { ChatScene } from "../../api/chat";

const scenes = ref<ChatScene[]>([]);
const agents = ref<Agent[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number>();

const form = reactive<ScenePayload>({
  sceneCode: "",
  sceneName: "",
  description: "",
  chatMode: "single",
  agentIds: [],
  enabled: 1
});

const enabledAgents = computed(() => agents.value.filter((agent) => agent.enabled === 1));

async function loadData() {
  loading.value = true;
  try {
    const [sceneList, agentList] = await Promise.all([listAdminScenes(), listAdminAgents()]);
    scenes.value = sceneList;
    agents.value = agentList;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, {
    sceneCode: "",
    sceneName: "",
    description: "",
    chatMode: "single",
    agentIds: [],
    enabled: 1
  });
  dialogVisible.value = true;
}

function openEdit(scene: ChatScene) {
  editingId.value = scene.id;
  Object.assign(form, {
    sceneCode: scene.sceneCode,
    sceneName: scene.sceneName,
    description: scene.description,
    chatMode: scene.chatMode,
    agentIds: scene.agents.map((agent) => agent.agentId),
    enabled: scene.enabled
  });
  dialogVisible.value = true;
}

async function saveScene() {
  if (!form.sceneName.trim()) {
    return ElMessage.warning("请输入场景名称");
  }
  if (form.agentIds.length === 0) {
    return ElMessage.warning("请选择至少一个 AI 助手");
  }
  try {
    const payload: ScenePayload = {
      sceneCode: form.sceneCode?.trim() || undefined,
      sceneName: form.sceneName.trim(),
      description: form.description.trim(),
      chatMode: form.chatMode,
      agentIds: form.agentIds,
      enabled: form.enabled
    };
    if (editingId.value) {
      await updateScene(editingId.value, payload);
      ElMessage.success("场景已更新");
    } else {
      await createScene(payload);
      ElMessage.success("场景已创建");
    }
    dialogVisible.value = false;
    await loadData();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "保存场景失败");
  }
}

function modeText(mode: string) {
  return mode === "team" ? "团队场景" : "单聊场景";
}

function modeTagType(mode: string) {
  return mode === "team" ? "warning" : "success";
}

function agentOptionLabel(agent: Agent) {
  const abilities: string[] = [];
  if (agent.toolsEnabled === 1) abilities.push("工具");
  if (agent.imageGenerationEnabled === 1) abilities.push("图片");
  return abilities.length ? `${agent.agentName}（${abilities.join(" / ")}）` : agent.agentName;
}

onMounted(loadData);
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p>AI</p>
        <h1>场景管理</h1>
      </div>
      <el-button type="primary" @click="openCreate">新增场景</el-button>
    </div>

    <el-card shadow="never" class="panel-card" v-loading="loading">
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="场景用于组织不同客户或业务下的一组 AI 助手"
        description="单聊场景下，每个 AI 保持独立上下文；团队场景下，场景内 AI 共用同一个上下文。图片生成等能力仍由具体 AI 的配置决定。"
      />

      <el-table :data="scenes" class="scene-table">
        <el-table-column prop="sceneName" label="场景名称" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.sceneName }}</strong>
            <small>{{ row.sceneCode }}</small>
          </template>
        </el-table-column>
        <el-table-column label="模式" width="120">
          <template #default="{ row }">
            <el-tag :type="modeTagType(row.chatMode)">{{ modeText(row.chatMode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="场景 AI" min-width="300">
          <template #default="{ row }">
            <div class="scene-agent-tags">
              <el-tag v-for="agent in row.agents" :key="agent.id" effect="plain">
                {{ agent.agentName }}
                <span v-if="agent.imageGenerationEnabled === 1"> · 图</span>
                <span v-if="agent.toolsEnabled === 1"> · 工具</span>
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑场景' : '新增场景'"
      width="720px"
      destroy-on-close
    >
      <el-form label-width="110px">
        <el-form-item label="场景名称">
          <el-input v-model="form.sceneName" placeholder="例如：餐饮企业获客诊断场景" />
        </el-form-item>
        <el-form-item label="场景编码">
          <el-input v-model="form.sceneCode" placeholder="不填则由系统自动生成" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="会话模式">
          <el-radio-group v-model="form.chatMode">
            <el-radio-button label="single">单聊场景</el-radio-button>
            <el-radio-button label="team">团队场景</el-radio-button>
          </el-radio-group>
          <p class="scene-form-tip">
            {{ form.chatMode === "team" ? "团队场景：一个会话内多个 AI 共用上下文，适合模拟 AI 团队开会。" : "单聊场景：每个 AI 有独立上下文，适合分别调试不同助手。" }}
          </p>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="说明这个场景服务什么客户、业务或流程" />
        </el-form-item>
        <el-form-item label="选择 AI">
          <el-select v-model="form.agentIds" multiple filterable placeholder="选择这个场景里可用的 AI 助手" style="width: 100%">
            <el-option
              v-for="agent in enabledAgents"
              :key="agent.id"
              :label="agentOptionLabel(agent)"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveScene">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.scene-table {
  margin-top: 18px;
}

.scene-table strong,
.scene-table small {
  display: block;
}

.scene-table small {
  margin-top: 4px;
  color: #94a3b8;
}

.scene-agent-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.scene-form-tip {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}
</style>
