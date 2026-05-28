<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createAgent,
  listAdminAgents,
  updateAgent,
  type Agent,
  type AgentPayload
} from "../../api/agents";
import {
  listModelApiKeys,
  listModelProviders,
  listModels,
  type AiModelItem,
  type ModelApiKey,
  type ModelProvider
} from "../../api/modelManagement";
import {
  listImageStorageConfigs,
  type ImageStorageConfig
} from "../../api/imageStorage";
import {
  createKnowledgeDocument,
  createWorkflow,
  deleteKnowledgeDocument,
  deleteMemory,
  deleteWorkflow,
  listKnowledgeDocuments,
  listMemories,
  listWorkflows,
  saveMemory,
  updateKnowledgeDocument,
  updateMemory,
  updateWorkflow,
  type KnowledgeDocument,
  type MemoryItem,
  type WorkflowItem
} from "../../api/workbench";

const agents = ref<Agent[]>([]);
const providers = ref<ModelProvider[]>([]);
const models = ref<AiModelItem[]>([]);
const apiKeys = ref<ModelApiKey[]>([]);
const imageStorageConfigs = ref<ImageStorageConfig[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number>();
const selectedProviderId = ref<number | null>(null);
const selectedImageProviderId = ref<number | null>(null);
const activeDialogTab = ref("base");

const memories = ref<MemoryItem[]>([]);
const knowledgeDocuments = ref<KnowledgeDocument[]>([]);
const workflows = ref<WorkflowItem[]>([]);
const resourceLoading = ref(false);
const editingMemoryId = ref<number>();
const editingKnowledgeId = ref<number>();
const editingWorkflowId = ref<number>();

const form = reactive<AgentPayload>(defaultForm());
const memoryForm = reactive(defaultMemoryForm());
const knowledgeForm = reactive(defaultKnowledgeForm());
const workflowForm = reactive(defaultWorkflowForm());

const enabledProviders = computed(() => providers.value.filter((provider) => provider.enabled === 1));
const currentAbilityType = computed(() => abilityTypeLabel(form));
const currentAbilityDescription = computed(() => {
  if (form.toolsEnabled === 1) {
    return "工具型智能体 = 大模型 + 记忆 / 知识库 / 工作流 + 本地文件和命令工具能力";
  }
  if (form.imageGenerationEnabled === 1) {
    return "多能力 AI = 对话大模型 + 图片生成工具，用对话质量承接业务，用图片模型完成素材生成";
  }
  if (hasEnhancedAbility(form)) {
    return "增强型 AI = 大模型 + 记忆 / 知识库 / 工作流，用于稳定业务表达和方法论复用";
  }
  return "普通大模型 = 只调用模型本身能力，适合基础问答和轻量对话";
});

const selectedModel = computed(() => models.value.find((item) => item.id === form.modelId));
const selectedApiKey = computed(() => apiKeys.value.find((item) => item.id === form.apiKeyId));
const selectedImageModel = computed(() => models.value.find((item) => item.id === form.imageModelId));
const selectedImageApiKey = computed(() => apiKeys.value.find((item) => item.id === form.imageApiKeyId));
const selectedImageStorageConfig = computed(() =>
  imageStorageConfigs.value.find((item) => item.id === form.imageStorageConfigId)
);
const modelApiOptions = computed(() => {
  if (!selectedProviderId.value) return [];
  return apiKeys.value.filter((item) =>
    item.providerId === selectedProviderId.value
    && item.enabled === 1
    && item.modelType !== "image_generation"
  );
});
const imageApiOptions = computed(() => {
  if (!selectedImageProviderId.value) return [];
  return apiKeys.value.filter((item) =>
    item.providerId === selectedImageProviderId.value
    && item.enabled === 1
    && item.modelType === "image_generation"
  );
});

function defaultForm(): AgentPayload {
  return {
    agentCode: "",
    agentName: "",
    description: "",
    systemPrompt: "",
    modelProvider: "",
    modelName: "",
    modelId: null,
    apiKeyId: null,
    temperature: 0.35,
    maxCompletionTokens: 4096,
    memoryEnabled: 0,
    knowledgeEnabled: 0,
    workflowEnabled: 0,
    toolsEnabled: 0,
    imageGenerationEnabled: 0,
    imageModelId: null,
    imageApiKeyId: null,
    imageStorageStrategy: "local",
    imageStorageConfigId: null,
    enabled: 1
  };
}

function defaultMemoryForm() {
  return { memoryKey: "", memoryValue: "", memoryType: "profile", enabled: 1 };
}

function defaultKnowledgeForm() {
  return { title: "", tags: "", content: "", enabled: 1 };
}

function defaultWorkflowForm() {
  return {
    workflowCode: "",
    workflowName: "",
    description: "",
    definitionJson: "",
    enabled: 1
  };
}

async function loadAgents() {
  loading.value = true;
  try {
    agents.value = await listAdminAgents();
  } finally {
    loading.value = false;
  }
}

async function loadModelOptions() {
  const [providerList, modelList, keyList, storageList] = await Promise.all([
    listModelProviders(),
    listModels(),
    listModelApiKeys(),
    listImageStorageConfigs()
  ]);
  providers.value = providerList;
  models.value = modelList;
  apiKeys.value = keyList;
  imageStorageConfigs.value = storageList;
}

function openCreate() {
  editingId.value = undefined;
  selectedProviderId.value = null;
  selectedImageProviderId.value = null;
  activeDialogTab.value = "base";
  Object.assign(form, defaultForm());
  clearResourceState();
  dialogVisible.value = true;
}

async function openEdit(agent: Agent) {
  editingId.value = agent.id;
  activeDialogTab.value = "base";
  Object.assign(form, defaultForm(), {
    ...agent,
    modelId: agent.modelId ?? null,
    apiKeyId: agent.apiKeyId ?? null,
    imageModelId: agent.imageModelId ?? null,
    imageApiKeyId: agent.imageApiKeyId ?? null,
    imageStorageStrategy: agent.imageStorageStrategy || "local",
    imageStorageConfigId: agent.imageStorageConfigId ?? null
  });
  selectedProviderId.value =
    apiKeys.value.find((item) => item.id === agent.apiKeyId)?.providerId
    ?? models.value.find((item) => item.id === agent.modelId)?.providerId
    ?? null;
  selectedImageProviderId.value =
    apiKeys.value.find((item) => item.id === agent.imageApiKeyId)?.providerId
    ?? models.value.find((item) => item.id === agent.imageModelId)?.providerId
    ?? null;
  dialogVisible.value = true;
  await loadAgentResources();
}

async function saveAgent() {
  ensureAgentCode();
  if (!form.agentName.trim() || !form.systemPrompt.trim()) {
    return ElMessage.warning("请填写名称和系统提示词");
  }
  if (!form.modelId || !form.apiKeyId) {
    return ElMessage.warning("请选择供应商和模型 API");
  }
  if (form.imageGenerationEnabled === 1 && (!form.imageModelId || !form.imageApiKeyId)) {
    return ElMessage.warning("开启图片生成能力时，请选择图片模型 API");
  }
  if (form.imageGenerationEnabled === 1 && !form.imageStorageConfigId) {
    return ElMessage.warning("开启图片生成能力时，请选择图片存储配置");
  }
  try {
    const payload = normalizedPayload();
    if (editingId.value) {
      await updateAgent(editingId.value, payload);
      ElMessage.success("AI 配置已更新");
    } else {
      const created = await createAgent(payload);
      editingId.value = created.id;
      ElMessage.success("AI 配置已创建");
    }
    await Promise.all([loadAgents(), loadAgentResources()]);
    dialogVisible.value = false;
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "保存失败");
  }
}

function normalizedPayload(): AgentPayload {
  ensureAgentCode();
  syncModelFieldsFromApiKey();
  syncImageModelFieldsFromApiKey();
  return {
    ...form,
    modelId: form.modelId || null,
    apiKeyId: form.apiKeyId || null,
    imageModelId: form.imageGenerationEnabled === 1 ? form.imageModelId || null : null,
    imageApiKeyId: form.imageGenerationEnabled === 1 ? form.imageApiKeyId || null : null,
    imageStorageConfigId: form.imageGenerationEnabled === 1 ? form.imageStorageConfigId || null : null,
    imageStorageStrategy: form.imageStorageStrategy || "local",
    modelProvider: form.modelProvider?.trim() || selectedModel.value?.providerCode || selectedApiKey.value?.providerCode || "",
    modelName: form.modelName?.trim() || selectedModel.value?.modelName || selectedApiKey.value?.modelName || ""
  };
}

function ensureAgentCode() {
  if (form.agentCode.trim()) {
    form.agentCode = normalizeAgentCode(form.agentCode);
    return;
  }
  form.agentCode = generateAgentCode(form.agentName);
}

function generateAgentCode(name: string) {
  const normalizedName = normalizeAgentCode(name);
  if (normalizedName && normalizedName !== "ai") {
    return uniqueAgentCode(`${normalizedName}_agent`);
  }

  const lowerName = name.toLowerCase();
  if (name.includes("获客") || lowerName.includes("lead")) {
    return uniqueAgentCode("lead_growth_operator");
  }
  if (name.includes("赋能") || name.includes("咨询") || lowerName.includes("advisor")) {
    return uniqueAgentCode("enterprise_ai_advisor");
  }
  if (name.includes("落地") || name.includes("执行") || lowerName.includes("workflow")) {
    return uniqueAgentCode("ai_implementation_agent");
  }
  return uniqueAgentCode(`ai_agent_${Date.now().toString(36)}`);
}

function normalizeAgentCode(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_+|_+$/g, "");
}

function uniqueAgentCode(baseCode: string) {
  const code = normalizeAgentCode(baseCode) || `ai_agent_${Date.now().toString(36)}`;
  const existedCodes = new Set(
    agents.value
      .filter((agent) => agent.id !== editingId.value)
      .map((agent) => agent.agentCode)
  );
  if (!existedCodes.has(code)) return code;

  let index = 2;
  while (existedCodes.has(`${code}_${index}`)) {
    index += 1;
  }
  return `${code}_${index}`;
}

async function loadAgentResources() {
  if (!editingId.value) {
    clearResourceState();
    return;
  }
  resourceLoading.value = true;
  try {
    const [memoryList, knowledgeList, workflowList] = await Promise.all([
      listMemories(editingId.value),
      listKnowledgeDocuments(editingId.value),
      listWorkflows(editingId.value)
    ]);
    memories.value = memoryList;
    knowledgeDocuments.value = knowledgeList;
    workflows.value = workflowList;
  } finally {
    resourceLoading.value = false;
  }
}

function clearResourceState() {
  memories.value = [];
  knowledgeDocuments.value = [];
  workflows.value = [];
  resetMemoryForm();
  resetKnowledgeForm();
  resetWorkflowForm();
}

function requireEditingAgentId() {
  if (!editingId.value) {
    ElMessage.warning("请先保存基础配置，再维护能力内容");
    return undefined;
  }
  return editingId.value;
}

function resetMemoryForm() {
  editingMemoryId.value = undefined;
  Object.assign(memoryForm, defaultMemoryForm());
}

function editMemory(item: MemoryItem) {
  editingMemoryId.value = item.id;
  Object.assign(memoryForm, {
    memoryKey: item.memoryKey,
    memoryValue: item.memoryValue,
    memoryType: item.memoryType || "profile",
    enabled: item.enabled
  });
}

async function submitMemory() {
  const agentId = requireEditingAgentId();
  if (!agentId) return;
  if (!memoryForm.memoryKey.trim() || !memoryForm.memoryValue.trim()) {
    return ElMessage.warning("请填写记忆键和值");
  }
  if (editingMemoryId.value) {
    await updateMemory(agentId, editingMemoryId.value, { ...memoryForm });
    ElMessage.success("长期记忆已更新");
  } else {
    await saveMemory(agentId, { ...memoryForm });
    ElMessage.success("长期记忆已新增");
  }
  resetMemoryForm();
  await loadAgentResources();
}

async function removeMemory(item: MemoryItem) {
  const agentId = requireEditingAgentId();
  if (!agentId) return;
  await ElMessageBox.confirm(`确认删除记忆「${item.memoryKey}」吗？`, "删除确认", { type: "warning" });
  await deleteMemory(agentId, item.id);
  ElMessage.success("长期记忆已删除");
  await loadAgentResources();
}

function resetKnowledgeForm() {
  editingKnowledgeId.value = undefined;
  Object.assign(knowledgeForm, defaultKnowledgeForm());
}

function editKnowledge(item: KnowledgeDocument) {
  editingKnowledgeId.value = item.id;
  Object.assign(knowledgeForm, {
    title: item.title,
    tags: item.tags,
    content: item.content,
    enabled: item.enabled
  });
}

async function submitKnowledge() {
  const agentId = requireEditingAgentId();
  if (!agentId) return;
  if (!knowledgeForm.title.trim() || !knowledgeForm.content.trim()) {
    return ElMessage.warning("请填写知识标题和内容");
  }
  if (editingKnowledgeId.value) {
    await updateKnowledgeDocument(agentId, editingKnowledgeId.value, { ...knowledgeForm });
    ElMessage.success("知识已更新");
  } else {
    await createKnowledgeDocument(agentId, { ...knowledgeForm });
    ElMessage.success("知识已新增");
  }
  resetKnowledgeForm();
  await loadAgentResources();
}

async function removeKnowledge(item: KnowledgeDocument) {
  const agentId = requireEditingAgentId();
  if (!agentId) return;
  await ElMessageBox.confirm(`确认删除知识「${item.title}」吗？`, "删除确认", { type: "warning" });
  await deleteKnowledgeDocument(agentId, item.id);
  ElMessage.success("知识已删除");
  await loadAgentResources();
}

function resetWorkflowForm() {
  editingWorkflowId.value = undefined;
  Object.assign(workflowForm, defaultWorkflowForm());
}

function editWorkflow(item: WorkflowItem) {
  editingWorkflowId.value = item.id;
  Object.assign(workflowForm, {
    workflowCode: item.workflowCode,
    workflowName: item.workflowName,
    description: item.description,
    definitionJson: item.definitionJson,
    enabled: item.enabled
  });
}

async function submitWorkflow() {
  const agentId = requireEditingAgentId();
  if (!agentId) return;
  if (!workflowForm.workflowCode.trim() || !workflowForm.workflowName.trim() || !workflowForm.definitionJson.trim()) {
    return ElMessage.warning("请填写工作流编码、名称和定义");
  }
  if (editingWorkflowId.value) {
    await updateWorkflow(agentId, editingWorkflowId.value, { ...workflowForm });
    ElMessage.success("工作流已更新");
  } else {
    await createWorkflow(agentId, { ...workflowForm });
    ElMessage.success("工作流已新增");
  }
  resetWorkflowForm();
  await loadAgentResources();
}

async function removeWorkflow(item: WorkflowItem) {
  const agentId = requireEditingAgentId();
  if (!agentId) return;
  await ElMessageBox.confirm(`确认删除工作流「${item.workflowName}」吗？`, "删除确认", { type: "warning" });
  await deleteWorkflow(agentId, item.id);
  ElMessage.success("工作流已删除");
  await loadAgentResources();
}

function hasEnhancedAbility(target: Pick<AgentPayload, "memoryEnabled" | "knowledgeEnabled" | "workflowEnabled" | "imageGenerationEnabled">) {
  return target.memoryEnabled === 1
    || target.knowledgeEnabled === 1
    || target.workflowEnabled === 1
    || target.imageGenerationEnabled === 1;
}

function abilityTypeLabel(target: Pick<AgentPayload, "toolsEnabled" | "memoryEnabled" | "knowledgeEnabled" | "workflowEnabled" | "imageGenerationEnabled">) {
  if (target.toolsEnabled === 1) return "工具型智能体";
  if (target.imageGenerationEnabled === 1) return "多能力 AI";
  if (hasEnhancedAbility(target)) return "增强型 AI";
  return "普通大模型";
}

function abilityTypeTag(target: Pick<AgentPayload, "toolsEnabled" | "memoryEnabled" | "knowledgeEnabled" | "workflowEnabled" | "imageGenerationEnabled">) {
  if (target.toolsEnabled === 1) return "warning";
  if (target.imageGenerationEnabled === 1) return "primary";
  if (hasEnhancedAbility(target)) return "success";
  return "info";
}

function modelLabel(modelId?: number | null) {
  const model = models.value.find((item) => item.id === modelId);
  if (!model) return "未选择模型";
  return `${model.providerName} / ${model.displayName || model.modelName}`;
}

function apiKeyLabel(apiKeyId?: number | null) {
  const apiKey = apiKeys.value.find((item) => item.id === apiKeyId);
  if (!apiKey) return "未指定 Key";
  return `${apiKey.modelDisplayName || apiKey.modelName} / ${apiKey.keyName}（${apiKeyValue(apiKey)}）`;
}

function imageModelLabel() {
  if (!selectedImageModel.value) return "未选择图片生成模型";
  return `${selectedImageModel.value.providerName} / ${selectedImageModel.value.displayName || selectedImageModel.value.modelName}`;
}

function imageApiKeyLabel() {
  if (!selectedImageApiKey.value) return "未指定图片生成 Key";
  return `${selectedImageApiKey.value.modelDisplayName || selectedImageApiKey.value.modelName} / ${selectedImageApiKey.value.keyName}（${apiKeyValue(selectedImageApiKey.value)}）`;
}

function imageStorageLabel() {
  if (!selectedImageStorageConfig.value) return "未指定图片存储";
  return `${selectedImageStorageConfig.value.storageName}（${storageTypeText(selectedImageStorageConfig.value.storageType)}）`;
}

function storageTypeText(type?: string) {
  const map: Record<string, string> = {
    local: "本地存储",
    object_storage: "图床/对象存储",
    oss: "阿里 OSS",
    cos: "腾讯 COS",
    qiniu: "七牛云",
    s3: "S3",
    custom: "自定义"
  };
  return map[type || ""] || type || "-";
}

function modelApiOptionLabel(apiKey: ModelApiKey) {
  return `${apiKey.modelDisplayName || apiKey.modelName} / ${apiKey.keyName}（${apiKeyValue(apiKey)}）`;
}

function apiKeyValue(apiKey: ModelApiKey) {
  return apiKey.apiKey || apiKey.apiKeyMask || "未填写 Key";
}

function syncModelFieldsFromApiKey() {
  const apiKey = selectedApiKey.value;
  if (!apiKey) return;
  selectedProviderId.value = apiKey.providerId;
  form.modelId = apiKey.modelId;
  form.modelProvider = apiKey.providerCode;
  form.modelName = apiKey.modelName;
}

function syncImageModelFieldsFromApiKey() {
  const apiKey = selectedImageApiKey.value;
  if (!apiKey) return;
  selectedImageProviderId.value = apiKey.providerId;
  form.imageModelId = apiKey.modelId;
}

watch(
  () => selectedProviderId.value,
  () => {
    if (!dialogVisible.value) return;
    if (!form.apiKeyId) return;
    const apiKey = apiKeys.value.find((item) => item.id === form.apiKeyId);
    if (apiKey && apiKey.providerId !== selectedProviderId.value) {
      form.apiKeyId = null;
      form.modelId = null;
      form.modelProvider = "";
      form.modelName = "";
    }
  }
);

watch(
  () => selectedImageProviderId.value,
  () => {
    if (!dialogVisible.value) return;
    if (!form.imageApiKeyId) return;
    const apiKey = apiKeys.value.find((item) => item.id === form.imageApiKeyId);
    if (apiKey && apiKey.providerId !== selectedImageProviderId.value) {
      form.imageApiKeyId = null;
      form.imageModelId = null;
    }
  }
);

watch(
  () => form.apiKeyId,
  () => syncModelFieldsFromApiKey()
);

watch(
  () => form.imageApiKeyId,
  () => syncImageModelFieldsFromApiKey()
);

watch(
  () => form.imageStorageConfigId,
  () => {
    if (selectedImageStorageConfig.value) {
      form.imageStorageStrategy = selectedImageStorageConfig.value.storageType;
    }
  }
);

watch(
  () => form.agentName,
  () => {
    if (!dialogVisible.value || editingId.value) return;
    form.agentCode = generateAgentCode(form.agentName);
  }
);

onMounted(async () => {
  await Promise.all([loadAgents(), loadModelOptions()]);
});
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p>AI</p>
        <h1>智能体管理</h1>
      </div>
      <el-button type="primary" @click="openCreate">新增 AI</el-button>
    </div>

    <el-card shadow="never" class="panel-card">
      <el-table v-loading="loading" :data="agents">
        <el-table-column prop="agentName" label="名称" min-width="150" />
        <el-table-column prop="agentCode" label="编码" min-width="180" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="能力类型" width="140">
          <template #default="{ row }">
            <el-tag :type="abilityTypeTag(row)">{{ abilityTypeLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ modelLabel(row.modelId) }}</template>
        </el-table-column>
        <el-table-column label="模型 API" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ apiKeyLabel(row.apiKeyId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      class="agent-config-dialog"
      :title="editingId ? '配置 AI 能力' : '新增 AI 能力'"
      width="1180px"
      top="4vh"
    >
      <el-tabs v-model="activeDialogTab" class="agent-config-tabs">
        <el-tab-pane label="基础配置" name="base">
          <div class="agent-config-panel">
            <div class="agent-identity-card">
              <div class="agent-avatar">AI</div>
              <div class="agent-identity-main">
                <p>AI Capability</p>
                <h2>{{ form.agentName || "新的 AI 能力" }}</h2>
                <span>{{ form.description || "为企业咨询、AI 赋能和获客增长配置一个可长期使用的 AI。" }}</span>
              </div>
              <div class="agent-identity-meta">
                <el-tag :type="abilityTypeTag(form)" effect="dark">{{ currentAbilityType }}</el-tag>
                <span>{{ modelLabel(form.modelId) }}</span>
              </div>
            </div>

            <el-alert
              class="agent-ability-alert"
              :title="currentAbilityType"
              :description="currentAbilityDescription"
              :type="abilityTypeTag(form)"
              show-icon
              :closable="false"
            />

            <el-form label-position="top">
              <div class="agent-config-grid">
                <div class="agent-config-main">
                  <div class="agent-section-card">
                    <div class="agent-section-head">
                      <div>
                        <p>Identity</p>
                        <h3>身份设定</h3>
                      </div>
                      <span>决定这个 AI 像谁、懂什么、如何表达。</span>
                    </div>
                    <div class="agent-form-two-columns">
                      <el-form-item label="内部编码（自动生成）">
                        <el-input
                          v-model="form.agentCode"
                          disabled
                          placeholder="可留空，保存时系统自动生成"
                        />
                        <div class="agent-field-tip">系统自动生成的唯一标识，用于接口和数据关联，不允许手动修改。</div>
                      </el-form-item>
                      <el-form-item label="名称">
                        <el-input v-model="form.agentName" placeholder="例如：AI获客操盘手" />
                      </el-form-item>
                    </div>
                    <el-form-item label="说明">
                      <el-input v-model="form.description" placeholder="一句话说明这个 AI 负责解决什么业务问题" />
                    </el-form-item>
                    <el-form-item label="系统提示词">
                      <el-input
                        v-model="form.systemPrompt"
                        class="agent-prompt-input"
                        type="textarea"
                        :rows="12"
                        placeholder="这里定义 AI 的身份、业务背景、回答方式和执行边界"
                      />
                    </el-form-item>
                  </div>

                  <div class="agent-section-card">
                    <div class="agent-section-head">
                      <div>
                        <p>Model</p>
                        <h3>模型绑定</h3>
                      </div>
                      <span>先选厂家，再选该厂家下已经维护好的模型 API。</span>
                    </div>
                    <div class="agent-form-two-columns">
                      <el-form-item label="供应商">
                        <el-select v-model="selectedProviderId" clearable filterable placeholder="先选择模型厂家">
                          <el-option
                            v-for="provider in enabledProviders"
                            :key="provider.id"
                            :label="provider.providerName"
                            :value="provider.id"
                          />
                        </el-select>
                      </el-form-item>
                      <el-form-item label="模型 API">
                        <el-select
                          v-model="form.apiKeyId"
                          clearable
                          filterable
                          :disabled="!selectedProviderId"
                          placeholder="选择模型 API 后自动带出模型和 Key"
                        >
                          <el-option
                            v-for="apiKey in modelApiOptions"
                            :key="apiKey.id"
                            :label="modelApiOptionLabel(apiKey)"
                            :value="apiKey.id"
                          />
                        </el-select>
                      </el-form-item>
                    </div>
                    <div class="agent-current-model">
                      <span>当前模型</span>
                      <strong>{{ modelLabel(form.modelId) }}</strong>
                      <small>{{ apiKeyLabel(form.apiKeyId) }}</small>
                    </div>
                  </div>
                </div>

                <aside class="agent-config-side">
                  <div class="agent-section-card compact">
                    <div class="agent-section-head">
                      <div>
                        <p>Abilities</p>
                        <h3>能力开关</h3>
                      </div>
                    </div>
                    <div class="ability-card" :class="{ active: form.memoryEnabled === 1 }">
                      <div>
                        <strong>长期记忆</strong>
                        <span>固定偏好、背景和长期信息</span>
                      </div>
                      <el-switch v-model="form.memoryEnabled" :active-value="1" :inactive-value="0" />
                    </div>
                    <div class="ability-card" :class="{ active: form.knowledgeEnabled === 1 }">
                      <div>
                        <strong>知识库</strong>
                        <span>业务资料、方法论和项目文档</span>
                      </div>
                      <el-switch v-model="form.knowledgeEnabled" :active-value="1" :inactive-value="0" />
                    </div>
                    <div class="ability-card" :class="{ active: form.workflowEnabled === 1 }">
                      <div>
                        <strong>工作流</strong>
                        <span>按固定步骤执行诊断、获客、成交和交付任务</span>
                      </div>
                      <el-switch v-model="form.workflowEnabled" :active-value="1" :inactive-value="0" />
                    </div>
                    <div class="ability-card dangerous" :class="{ active: form.toolsEnabled === 1 }">
                      <div>
                        <strong>本地工具</strong>
                        <span>读取、整理和生成本地企业资料、表格与方案文档</span>
                      </div>
                      <el-switch v-model="form.toolsEnabled" :active-value="1" :inactive-value="0" />
                    </div>
                    <div class="ability-card image-ability" :class="{ active: form.imageGenerationEnabled === 1 }">
                      <div>
                        <strong>图片生成</strong>
                        <span>把文生图作为工具能力交给当前 AI 使用</span>
                      </div>
                      <el-switch v-model="form.imageGenerationEnabled" :active-value="1" :inactive-value="0" />
                    </div>
                  </div>

                  <div class="agent-section-card compact">
                    <div class="agent-section-head">
                      <div>
                        <p>Generation</p>
                        <h3>生成参数</h3>
                      </div>
                    </div>
                    <el-form-item>
                      <template #label>
                        <div class="temperature-label">
                          <span>温度</span>
                          <em>数值越低越稳定严谨，越高越发散有创意</em>
                        </div>
                      </template>
                      <div class="temperature-control">
                        <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.05" />
                      </div>
                    </el-form-item>
                    <el-form-item label="输出上限">
                      <el-input-number v-model="form.maxCompletionTokens" :min="1" :max="65536" />
                    </el-form-item>
                    <div class="agent-enable-row">
                      <div>
                        <strong>启用状态</strong>
                        <span>关闭后对话入口不可选择该 AI</span>
                      </div>
                      <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
                    </div>
                  </div>

                </aside>
              </div>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="图片生成" name="image">
          <div class="agent-config-panel">
            <div class="image-config-layout">
              <el-card shadow="never" class="image-config-hero">
                <div>
                  <p>Image Generation</p>
                  <h2>图片生成能力</h2>
                  <span>
                    这里配置当前 AI 调用哪个图片生成服务。对话模型仍负责理解需求和整理提示词，图片模型只作为工具执行生成。
                  </span>
                </div>
                <el-switch
                  v-model="form.imageGenerationEnabled"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="已开启"
                  inactive-text="未开启"
                />
              </el-card>

              <el-card shadow="never" class="image-config-card">
                <template #header>图片工具配置</template>
                <el-form label-width="120px">
                  <el-form-item label="图片生成服务">
                    <el-select
                      v-model="selectedImageProviderId"
                      clearable
                      filterable
                      :disabled="form.imageGenerationEnabled !== 1"
                      placeholder="选择图片生成服务，例如智谱 AI"
                    >
                      <el-option
                        v-for="provider in enabledProviders"
                        :key="provider.id"
                        :label="provider.providerName"
                        :value="provider.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="图片生成 API">
                    <el-select
                      v-model="form.imageApiKeyId"
                      clearable
                      filterable
                      :disabled="form.imageGenerationEnabled !== 1 || !selectedImageProviderId"
                      placeholder="选择图片生成模型 API"
                    >
                      <el-option
                        v-for="apiKey in imageApiOptions"
                        :key="apiKey.id"
                        :label="modelApiOptionLabel(apiKey)"
                        :value="apiKey.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="图片存储">
                    <el-select
                      v-model="form.imageStorageConfigId"
                      clearable
                      filterable
                      :disabled="form.imageGenerationEnabled !== 1"
                      placeholder="选择图片存储配置"
                    >
                      <el-option
                        v-for="storage in imageStorageConfigs.filter((item) => item.enabled === 1)"
                        :key="storage.id"
                        :label="`${storage.storageName}（${storageTypeText(storage.storageType)}）`"
                        :value="storage.id"
                      >
                        <span>{{ storage.storageName }}</span>
                        <small class="image-storage-option">{{ storageTypeText(storage.storageType) }} · {{ storage.rootPath || storage.bucketName || "-" }}</small>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-form>
              </el-card>

              <el-card shadow="never" class="image-config-card">
                <template #header>当前图片工具</template>
                <div class="agent-current-model image-current-model">
                  <span>图片模型</span>
                  <strong>{{ imageModelLabel() }}</strong>
                  <small>{{ imageApiKeyLabel() }}</small>
                  <small>{{ imageStorageLabel() }}</small>
                </div>
                <el-alert
                  class="image-config-tip"
                  type="info"
                  :closable="false"
                  title="图片不会直接塞入模型上下文"
                  description="后续生成图片时，聊天窗口展示图片，模型上下文只保留图片编号和短摘要，避免影响多轮对话质量。"
                />
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="长期记忆" name="memory">
          <el-alert v-if="!editingId" type="info" show-icon :closable="false" title="请先保存基础配置，再维护长期记忆" />
          <div v-else v-loading="resourceLoading" class="agent-resource-grid">
            <el-card shadow="never">
              <template #header>{{ editingMemoryId ? "编辑长期记忆" : "新增长期记忆" }}</template>
              <el-form label-width="88px">
                <el-form-item label="键"><el-input v-model="memoryForm.memoryKey" placeholder="例如：preferred_style" /></el-form-item>
                <el-form-item label="类型"><el-input v-model="memoryForm.memoryType" placeholder="profile/business/style" /></el-form-item>
                <el-form-item label="内容"><el-input v-model="memoryForm.memoryValue" type="textarea" :rows="5" /></el-form-item>
                <el-form-item label="状态"><el-switch v-model="memoryForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitMemory">{{ editingMemoryId ? "保存" : "新增" }}</el-button>
                  <el-button @click="resetMemoryForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
            <el-card shadow="never">
              <template #header>已创建长期记忆</template>
              <el-table :data="memories" height="360">
                <el-table-column prop="memoryKey" label="键" width="160" />
                <el-table-column prop="memoryValue" label="内容" show-overflow-tooltip />
                <el-table-column label="状态" width="88">
                  <template #default="{ row }">
                    <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? "启用" : "停用" }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="editMemory(row)">编辑</el-button>
                    <el-button link type="danger" @click="removeMemory(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="知识库" name="knowledge">
          <el-alert v-if="!editingId" type="info" show-icon :closable="false" title="请先保存基础配置，再维护知识库" />
          <div v-else v-loading="resourceLoading" class="agent-resource-grid">
            <el-card shadow="never">
              <template #header>{{ editingKnowledgeId ? "编辑知识" : "新增知识" }}</template>
              <el-form label-width="88px">
                <el-form-item label="标题"><el-input v-model="knowledgeForm.title" /></el-form-item>
                <el-form-item label="标签"><el-input v-model="knowledgeForm.tags" placeholder="多个标签用逗号分隔" /></el-form-item>
                <el-form-item label="内容"><el-input v-model="knowledgeForm.content" type="textarea" :rows="7" /></el-form-item>
                <el-form-item label="状态"><el-switch v-model="knowledgeForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitKnowledge">{{ editingKnowledgeId ? "保存" : "新增" }}</el-button>
                  <el-button @click="resetKnowledgeForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
            <el-card shadow="never">
              <template #header>已创建知识</template>
              <el-table :data="knowledgeDocuments" height="420">
                <el-table-column prop="title" label="标题" width="180" />
                <el-table-column prop="tags" label="标签" width="140" show-overflow-tooltip />
                <el-table-column prop="content" label="内容" show-overflow-tooltip />
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="editKnowledge(row)">编辑</el-button>
                    <el-button link type="danger" @click="removeKnowledge(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="工作流" name="workflow">
          <el-alert v-if="!editingId" type="info" show-icon :closable="false" title="请先保存基础配置，再维护工作流" />
          <div v-else v-loading="resourceLoading" class="agent-resource-grid">
            <el-card shadow="never">
              <template #header>{{ editingWorkflowId ? "编辑工作流" : "新增工作流" }}</template>
              <el-form label-width="88px">
                <el-form-item label="编码"><el-input v-model="workflowForm.workflowCode" placeholder="例如：consulting_analysis" /></el-form-item>
                <el-form-item label="名称"><el-input v-model="workflowForm.workflowName" /></el-form-item>
                <el-form-item label="说明"><el-input v-model="workflowForm.description" /></el-form-item>
                <el-form-item label="定义"><el-input v-model="workflowForm.definitionJson" type="textarea" :rows="8" /></el-form-item>
                <el-form-item label="状态"><el-switch v-model="workflowForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitWorkflow">{{ editingWorkflowId ? "保存" : "新增" }}</el-button>
                  <el-button @click="resetWorkflowForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>
            <el-card shadow="never">
              <template #header>已创建工作流</template>
              <el-table :data="workflows" height="420">
                <el-table-column prop="workflowCode" label="编码" width="160" />
                <el-table-column prop="workflowName" label="名称" width="160" />
                <el-table-column prop="description" label="说明" show-overflow-tooltip />
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="editWorkflow(row)">编辑</el-button>
                    <el-button link type="danger" @click="removeWorkflow(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAgent">保存基础配置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
:deep(.agent-config-dialog) {
  display: flex;
  flex-direction: column;
  height: 88vh;
  max-height: 88vh;
  overflow: hidden;
  border-radius: 18px;
}

:deep(.agent-config-dialog .el-dialog__header) {
  padding: 22px 28px 12px;
  border-bottom: 1px solid #eef2f7;
}

:deep(.agent-config-dialog .el-dialog__title) {
  color: #06162f;
  font-size: 20px;
  font-weight: 800;
}

:deep(.agent-config-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 28px 22px;
  background: #f5f7fb;
}

:deep(.agent-config-dialog .el-dialog__footer) {
  padding: 16px 28px;
  border-top: 1px solid #eef2f7;
  background: #fff;
}

.agent-config-tabs {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  --el-color-primary: #2563eb;
}

.agent-config-tabs :deep(.el-tabs__content) {
  flex: 1;
}

.agent-config-panel {
  padding-top: 16px;
}

.agent-identity-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px;
  border: 1px solid #dbe7ff;
  border-radius: 22px;
  background:
    radial-gradient(circle at right top, rgba(37, 99, 235, 0.16), transparent 34%),
    linear-gradient(135deg, #ffffff 0%, #edf5ff 100%);
  box-shadow: 0 18px 50px rgba(15, 35, 80, 0.08);
}

.agent-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 20px;
  color: #fff;
  font-weight: 900;
  background: linear-gradient(135deg, #2563eb, #00a3c7);
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.24);
}

.agent-identity-main {
  flex: 1;
  min-width: 0;
}

.agent-identity-main p,
.agent-section-head p {
  margin: 0;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.agent-identity-main h2 {
  margin: 5px 0 6px;
  color: #06162f;
  font-size: 28px;
  line-height: 1.2;
}

.agent-identity-main span,
.agent-identity-meta span,
.agent-section-head span,
.ability-card span,
.agent-enable-row span,
.agent-current-model small {
  color: #60718a;
  line-height: 1.6;
}

.agent-identity-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  max-width: 280px;
  text-align: right;
}

.agent-ability-alert {
  margin: 16px 0;
  border: none;
  border-radius: 16px;
}

.agent-config-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 18px;
}

.agent-config-main,
.agent-config-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.agent-section-card {
  padding: 20px;
  border: 1px solid #e2eaf5;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 12px 34px rgba(15, 35, 80, 0.05);
}

.agent-section-card.compact {
  padding: 18px;
}

.agent-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.agent-section-head h3 {
  margin: 4px 0 0;
  color: #06162f;
  font-size: 18px;
}

.agent-form-two-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.agent-prompt-input :deep(.el-textarea__inner) {
  line-height: 1.7;
  border-radius: 14px;
  font-family: Consolas, "Microsoft YaHei", monospace;
}

.agent-field-tip {
  margin-top: 6px;
  color: #8593a6;
  font-size: 12px;
  line-height: 1.4;
}

.temperature-control {
  width: 100%;
}

.temperature-label {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.temperature-label em {
  color: #8a98aa;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.agent-current-model {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px dashed #bcd3ff;
  border-radius: 16px;
  background: #f6f9ff;
}

.agent-current-model span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.agent-current-model strong {
  color: #06162f;
}

.ability-card,
.agent-enable-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid #e3ebf5;
  border-radius: 16px;
  background: #f8fafc;
}

.ability-card + .ability-card {
  margin-top: 10px;
}

.ability-card.active {
  border-color: #9dd7b4;
  background: #f0fbf3;
}

.ability-card.dangerous.active {
  border-color: #f4c27c;
  background: #fff7ed;
}

.ability-card.image-ability.active {
  border-color: #a7d7ff;
  background: #eff6ff;
}

.image-config-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.image-config-hero {
  grid-column: 1 / -1;
  border-radius: 20px;
  background:
    radial-gradient(circle at right top, rgba(37, 99, 235, 0.16), transparent 36%),
    linear-gradient(135deg, #ffffff 0%, #edf5ff 100%);
}

.image-config-hero :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.image-config-hero p {
  margin: 0;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.image-config-hero h2 {
  margin: 6px 0 8px;
  color: #06162f;
}

.image-config-hero span,
.image-config-card {
  color: #60718a;
  line-height: 1.7;
}

.image-config-card {
  border-radius: 20px;
}

.image-config-card:first-of-type {
  min-width: 0;
}

.image-current-model {
  margin-bottom: 14px;
}

.image-storage-option {
  margin-left: 8px;
  color: #94a3b8;
}

.image-config-tip {
  border-radius: 14px;
}

.image-tool-card.active {
  border-color: #a7d7ff;
  background:
    radial-gradient(circle at right top, rgba(37, 99, 235, 0.12), transparent 32%),
    #ffffff;
}

.image-tool-desc {
  margin: -4px 0 14px;
  color: #60718a;
  line-height: 1.7;
}

.image-current-model {
  margin-top: 6px;
}

.ability-card strong,
.agent-enable-row strong {
  display: block;
  margin-bottom: 3px;
  color: #06162f;
}

.agent-enable-row {
  margin-top: 8px;
}

.agent-resource-grid {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 18px;
  padding-top: 16px;
}

@media (max-width: 1180px) {
  .agent-config-grid,
  .agent-resource-grid,
  .image-config-layout {
    grid-template-columns: 1fr;
  }

  .agent-identity-card,
  .agent-section-head {
    flex-direction: column;
  }

  .agent-identity-meta {
    align-items: flex-start;
    max-width: none;
    text-align: left;
  }
}
</style>
