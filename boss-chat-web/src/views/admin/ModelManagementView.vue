<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createModel,
  createModelApiKey,
  createModelProvider,
  deleteModel,
  deleteModelApiKey,
  listModelApiKeys,
  listModelProviders,
  listModels,
  updateModel,
  updateModelApiKey,
  updateModelProvider,
  type AiModelItem,
  type AiModelPayload,
  type ModelApiKey,
  type ModelApiKeyPayload,
  type ModelProvider,
  type ModelProviderPayload
} from "../../api/modelManagement";

const loading = ref(false);
const providers = ref<ModelProvider[]>([]);
const models = ref<AiModelItem[]>([]);
const apiKeys = ref<ModelApiKey[]>([]);

const detailDrawerVisible = ref(false);
const providerDialogVisible = ref(false);
const modelDialogVisible = ref(false);
const apiKeyDialogVisible = ref(false);
const selectedProviderId = ref<number>();
const editingProviderId = ref<number>();
const editingModelId = ref<number>();
const editingApiKeyId = ref<number>();

const providerForm = reactive<ModelProviderPayload>(defaultProviderForm());
const modelForm = reactive<AiModelPayload>(defaultModelForm());
const apiKeyForm = reactive<ModelApiKeyPayload>(defaultApiKeyForm());

interface ModelOfficialMeta {
  billing: "free" | "paid" | "unknown";
  docsUrl?: string;
}

const modelOfficialMeta: Record<string, ModelOfficialMeta> = {
  "zhipu:glm-4.5-flash": {
    billing: "free",
    docsUrl: "https://docs.bigmodel.cn/cn/guide/models/free/glm-4.5-flash"
  },
  "zhipu:glm-image": {
    billing: "paid",
    docsUrl: "https://docs.bigmodel.cn/cn/guide/models/image-generation/glm-image"
  },
  "kimi:kimi-k2.6": {
    billing: "paid",
    docsUrl: "https://platform.kimi.com/docs/api/overview"
  },
  "openai:gpt-5.5": {
    billing: "free",
    docsUrl: "https://platform.openai.com/docs/models"
  }
};

const enabledProviders = computed(() => providers.value.filter((provider) => provider.enabled === 1));
const selectedProvider = computed(() => providers.value.find((provider) => provider.id === selectedProviderId.value));
const selectedProviderModels = computed(() => {
  if (!selectedProviderId.value) return [];
  return models.value.filter((model) => model.providerId === selectedProviderId.value);
});
const apiKeyModelOptions = computed(() => {
  if (!apiKeyForm.providerId) return [];
  return models.value.filter((model) => model.providerId === apiKeyForm.providerId && model.enabled === 1);
});

function defaultProviderForm(): ModelProviderPayload {
  return {
    providerCode: "",
    providerName: "",
    baseUrl: "",
    authType: "bearer",
    enabled: 1,
    remark: ""
  };
}

function defaultModelForm(): AiModelPayload {
  return {
    providerId: null,
    modelName: "",
    displayName: "",
    modelType: "chat",
    apiPath: "/chat/completions",
    billingType: "unknown",
    officialDocUrl: "",
    compatibilityProfile: "",
    contextWindow: 0,
    supportsStream: 1,
    supportsTools: 0,
    supportsVision: 0,
    enabled: 1,
    remark: ""
  };
}

function defaultApiKeyForm(): ModelApiKeyPayload {
  return {
    providerId: null,
    modelId: null,
    keyName: "",
    keyType: "paid",
    apiKey: "",
    priority: 100,
    enabled: 1,
    remark: ""
  };
}

async function loadAll() {
  loading.value = true;
  try {
    const [providerList, modelList, keyList] = await Promise.all([
      listModelProviders(),
      listModels(),
      listModelApiKeys()
    ]);
    providers.value = providerList;
    models.value = modelList;
    apiKeys.value = keyList;
    if (selectedProviderId.value && !providerList.some((provider) => provider.id === selectedProviderId.value)) {
      selectedProviderId.value = undefined;
      detailDrawerVisible.value = false;
    }
  } finally {
    loading.value = false;
  }
}

function openProviderDetail(provider: ModelProvider) {
  selectedProviderId.value = provider.id;
  detailDrawerVisible.value = true;
}

function openCreateProvider() {
  editingProviderId.value = undefined;
  Object.assign(providerForm, defaultProviderForm());
  providerDialogVisible.value = true;
}

function openEditProvider(provider: ModelProvider) {
  editingProviderId.value = provider.id;
  Object.assign(providerForm, provider);
  providerDialogVisible.value = true;
}

async function saveProvider() {
  if (!providerForm.providerCode.trim() || !providerForm.providerName.trim()) {
    return ElMessage.warning("请填写供应商编码和名称");
  }
  try {
    if (editingProviderId.value) {
      await updateModelProvider(editingProviderId.value, providerForm);
      ElMessage.success("供应商已更新");
    } else {
      await createModelProvider(providerForm);
      ElMessage.success("供应商已创建");
    }
    providerDialogVisible.value = false;
    await loadAll();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "供应商保存失败");
  }
}

function openCreateModel(provider?: ModelProvider) {
  editingModelId.value = undefined;
  Object.assign(modelForm, {
    ...defaultModelForm(),
    providerId: provider?.id ?? selectedProviderId.value ?? null
  });
  modelDialogVisible.value = true;
}

function openEditModel(model: AiModelItem) {
  editingModelId.value = model.id;
  Object.assign(modelForm, {
    providerId: model.providerId,
    modelName: model.modelName,
    displayName: model.displayName,
    modelType: model.modelType || "chat",
    apiPath: model.apiPath || defaultModelApiPath(model.modelType),
    billingType: model.billingType || "unknown",
    officialDocUrl: model.officialDocUrl || "",
    compatibilityProfile: model.compatibilityProfile || "",
    contextWindow: model.contextWindow || 0,
    supportsStream: model.supportsStream,
    supportsTools: model.supportsTools,
    supportsVision: model.supportsVision,
    enabled: model.enabled,
    remark: model.remark || ""
  });
  modelDialogVisible.value = true;
}

async function saveModel() {
  if (!modelForm.providerId || !modelForm.modelName.trim()) {
    return ElMessage.warning("请选择供应商并填写模型调用名");
  }
  if (!modelForm.apiPath.trim()) {
    return ElMessage.warning("请填写模型接口路径");
  }
  try {
    if (editingModelId.value) {
      await updateModel(editingModelId.value, modelForm);
      ElMessage.success("模型已更新");
    } else {
      await createModel(modelForm);
      ElMessage.success("模型已创建");
    }
    modelDialogVisible.value = false;
    await loadAll();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "模型保存失败");
  }
}

async function removeModel(model: AiModelItem) {
  try {
    await ElMessageBox.confirm(`确认删除模型「${model.displayName || model.modelName}」吗？相关 API Key 也会一起删除。`, "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
    await deleteModel(model.id);
    ElMessage.success("模型已删除");
    await loadAll();
  } catch (error: any) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error?.response?.data?.message || "模型删除失败");
  }
}

function openCreateApiKey(model?: AiModelItem) {
  editingApiKeyId.value = undefined;
  Object.assign(apiKeyForm, {
    ...defaultApiKeyForm(),
    providerId: model?.providerId ?? selectedProviderId.value ?? null,
    modelId: model?.id ?? null
  });
  apiKeyDialogVisible.value = true;
}

function openEditApiKey(apiKey: ModelApiKey) {
  editingApiKeyId.value = apiKey.id;
  Object.assign(apiKeyForm, {
    providerId: apiKey.providerId,
    modelId: apiKey.modelId,
    keyName: apiKey.keyName,
    keyType: apiKey.keyType,
    apiKey: "",
    priority: apiKey.priority,
    enabled: apiKey.enabled,
    remark: apiKey.remark || ""
  });
  apiKeyDialogVisible.value = true;
}

async function saveApiKey() {
  if (!apiKeyForm.providerId || !apiKeyForm.modelId || !apiKeyForm.keyName.trim()) {
    return ElMessage.warning("请选择供应商、模型并填写 Key 名称");
  }
  if (!editingApiKeyId.value && !apiKeyForm.apiKey.trim()) {
    return ElMessage.warning("新增 API Key 时必须填写 Key 内容");
  }
  try {
    if (editingApiKeyId.value) {
      await updateModelApiKey(editingApiKeyId.value, apiKeyForm);
      ElMessage.success("模型 API Key 已更新");
    } else {
      await createModelApiKey(apiKeyForm);
      ElMessage.success("模型 API Key 已创建");
    }
    apiKeyDialogVisible.value = false;
    await loadAll();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "API Key 保存失败");
  }
}

async function removeApiKey(apiKey: ModelApiKey) {
  try {
    await ElMessageBox.confirm(`确认删除 Key「${apiKey.keyName}」吗？`, "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
    await deleteModelApiKey(apiKey.id);
    ElMessage.success("API Key 已删除");
    await loadAll();
  } catch (error: any) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error?.response?.data?.message || "API Key 删除失败");
  }
}

function providerModelCount(providerId: number) {
  return models.value.filter((model) => model.providerId === providerId).length;
}

function providerApiKeyCount(providerId: number) {
  return apiKeys.value.filter((apiKey) => apiKey.providerId === providerId).length;
}

function modelApiKeys(modelId: number) {
  return apiKeys.value.filter((apiKey) => apiKey.modelId === modelId);
}

function defaultModelApiPath(modelType?: string) {
  switch (modelType) {
    case "image_generation":
      return "/images/generations";
    case "embedding":
      return "/embeddings";
    case "chat":
    default:
      return "/chat/completions";
  }
}

function modelMeta(model: AiModelItem) {
  return modelOfficialMeta[`${model.providerCode}:${model.modelName}`.toLowerCase()];
}

function statusTag(enabled: number) {
  return enabled === 1 ? "success" : "info";
}

function statusText(enabled: number) {
  return enabled === 1 ? "启用" : "停用";
}

function billingTag(value?: string) {
  const type = value || "unknown";
  if (type === "free") return "success";
  if (type === "paid") return "warning";
  return "info";
}

function billingText(value?: string) {
  const type = value || "unknown";
  if (type === "free") return "免费";
  if (type === "paid") return "付费";
  return "未知";
}

function modelBillingType(model: AiModelItem) {
  return modelMeta(model)?.billing || model.billingType || "unknown";
}

function modelDocsUrl(model: AiModelItem) {
  return model.officialDocUrl || modelMeta(model)?.docsUrl || "";
}

watch(
  () => apiKeyForm.providerId,
  () => {
    if (!apiKeyDialogVisible.value) return;
    const selectedModel = models.value.find((model) => model.id === apiKeyForm.modelId);
    if (selectedModel && selectedModel.providerId !== apiKeyForm.providerId) {
      apiKeyForm.modelId = null;
    }
  }
);

watch(
  () => modelForm.modelType,
  (modelType) => {
    if (!modelDialogVisible.value) return;
    const knownDefaults = ["/chat/completions", "/images/generations", "/embeddings", ""];
    if (knownDefaults.includes(modelForm.apiPath)) {
      modelForm.apiPath = defaultModelApiPath(modelType);
    }
  }
);

onMounted(loadAll);
</script>

<template>
  <section class="model-page">
    <div class="page-heading">
      <div>
        <p>AI</p>
        <h1>模型管理</h1>
      </div>
      <el-button type="primary" @click="openCreateProvider">新增供应商</el-button>
    </div>

    <div class="model-panel">
      <el-alert
        title="统一管理模型供应商、模型、API Key 和兼容配置"
        description="不支持工具调用的模型请关闭工具能力，避免请求参数冲突；特殊模型可以通过兼容模式处理请求参数。"
        type="info"
        show-icon
        :closable="false"
      />

      <el-table v-loading="loading" :data="providers" class="provider-table">
        <el-table-column prop="providerName" label="供应商" min-width="160" />
        <el-table-column prop="providerCode" label="编码" min-width="120" />
        <el-table-column prop="baseUrl" label="接口地址" min-width="280" show-overflow-tooltip />
        <el-table-column label="资源" width="170">
          <template #default="{ row }">
            <div class="resource-count">
              <span>{{ providerModelCount(row.id) }} 个模型</span>
              <span>{{ providerApiKeyCount(row.id) }} 个 Key</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.enabled)">{{ statusText(row.enabled) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openProviderDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEditProvider(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="detailDrawerVisible" size="72%" :title="selectedProvider?.providerName || '供应商详情'">
      <template v-if="selectedProvider">
        <div class="provider-detail-head">
          <div>
            <p class="detail-eyebrow">供应商</p>
            <h2>{{ selectedProvider.providerName }}</h2>
            <span>{{ selectedProvider.providerCode }}</span>
          </div>
          <div class="detail-actions">
            <el-button @click="openEditProvider(selectedProvider)">编辑供应商</el-button>
            <el-button type="primary" @click="openCreateModel(selectedProvider)">新增模型</el-button>
          </div>
        </div>

        <el-descriptions :column="2" border class="provider-descriptions">
          <el-descriptions-item label="接口地址">{{ selectedProvider.baseUrl || "-" }}</el-descriptions-item>
          <el-descriptions-item label="鉴权方式">{{ selectedProvider.authType || "-" }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(selectedProvider.enabled)">{{ statusText(selectedProvider.enabled) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ selectedProvider.remark || "-" }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-section-title">
          <p>模型列表</p>
          <span>模型接口、计费类型、兼容模式和 Key 绑定情况</span>
        </div>

        <el-empty v-if="selectedProviderModels.length === 0" description="暂无模型">
          <el-button type="primary" @click="openCreateModel(selectedProvider)">新增模型</el-button>
        </el-empty>

        <div v-else class="model-detail-list">
          <div v-for="model in selectedProviderModels" :key="model.id" class="model-detail-card">
            <div class="model-detail-header">
              <div>
                <div class="model-title-row">
                  <h3>{{ model.displayName || model.modelName }}</h3>
                  <el-tag :type="billingTag(modelBillingType(model))">{{ billingText(modelBillingType(model)) }}</el-tag>
                  <el-tag :type="statusTag(model.enabled)">{{ statusText(model.enabled) }}</el-tag>
                </div>
                <p>{{ model.modelName }}</p>
              </div>
              <div class="model-actions">
                <el-button link type="primary" @click="openEditModel(model)">编辑模型</el-button>
                <el-button link type="primary" @click="openCreateApiKey(model)">新增 Key</el-button>
                <el-button link type="danger" @click="removeModel(model)">删除模型</el-button>
              </div>
            </div>

            <div class="model-meta">
              <span>类型：{{ model.modelType || "-" }}</span>
              <span>路径：{{ model.apiPath || "-" }}</span>
              <span>上下文：{{ model.contextWindow || 0 }}</span>
              <span>流式：{{ model.supportsStream === 1 ? "支持" : "关闭" }}</span>
              <span>工具：{{ model.supportsTools === 1 ? "支持" : "关闭" }}</span>
              <span>视觉：{{ model.supportsVision === 1 ? "支持" : "关闭" }}</span>
            </div>
            <div class="model-note">
              <el-link v-if="modelDocsUrl(model)" :href="modelDocsUrl(model)" target="_blank" type="primary">官方文档</el-link>
              <span v-if="model.compatibilityProfile">兼容模式：{{ model.compatibilityProfile }}</span>
              <span v-if="model.remark">{{ model.remark }}</span>
            </div>

            <el-table :data="modelApiKeys(model.id)" size="small" empty-text="暂无 API Key">
              <el-table-column prop="keyName" label="Key 名称" min-width="160" />
              <el-table-column prop="keyType" label="类型" width="100" />
              <el-table-column prop="apiKeyMask" label="Key 掩码" min-width="180" />
              <el-table-column prop="priority" label="优先级" width="90" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="statusTag(row.enabled)">{{ statusText(row.enabled) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEditApiKey(row)">编辑</el-button>
                  <el-button link type="danger" @click="removeApiKey(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="providerDialogVisible" :title="editingProviderId ? '编辑供应商' : '新增供应商'" width="640px">
      <el-form label-width="120px">
        <el-form-item label="供应商编码"><el-input v-model="providerForm.providerCode" placeholder="openai" /></el-form-item>
        <el-form-item label="供应商名称"><el-input v-model="providerForm.providerName" placeholder="OpenAI" /></el-form-item>
        <el-form-item label="接口地址"><el-input v-model="providerForm.baseUrl" placeholder="https://api.openai.com/v1" /></el-form-item>
        <el-form-item label="鉴权方式"><el-input v-model="providerForm.authType" placeholder="bearer" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="providerForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="providerForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="providerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProvider">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="modelDialogVisible" :title="editingModelId ? '编辑模型' : '新增模型'" width="700px">
      <el-form label-width="140px">
        <el-form-item label="供应商">
          <el-select v-model="modelForm.providerId" filterable placeholder="请选择供应商">
            <el-option v-for="provider in enabledProviders" :key="provider.id" :label="provider.providerName" :value="provider.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型调用名"><el-input v-model="modelForm.modelName" placeholder="gpt-5.5" /></el-form-item>
        <el-form-item label="显示名称"><el-input v-model="modelForm.displayName" placeholder="ChatGPT 5.5" /></el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="modelForm.modelType">
            <el-option label="对话" value="chat" />
            <el-option label="图片生成" value="image_generation" />
            <el-option label="向量" value="embedding" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口路径"><el-input v-model="modelForm.apiPath" /></el-form-item>
        <el-form-item label="计费类型">
          <el-select v-model="modelForm.billingType">
            <el-option label="免费" value="free" />
            <el-option label="付费" value="paid" />
            <el-option label="未知" value="unknown" />
          </el-select>
        </el-form-item>
        <el-form-item label="官方文档"><el-input v-model="modelForm.officialDocUrl" /></el-form-item>
        <el-form-item label="兼容模式"><el-input v-model="modelForm.compatibilityProfile" placeholder="openai_gpt5 / kimi_k2" /></el-form-item>
        <el-form-item label="上下文窗口"><el-input-number v-model="modelForm.contextWindow" :min="0" :step="1000" /></el-form-item>
        <el-form-item label="能力">
          <el-checkbox v-model="modelForm.supportsStream" :true-value="1" :false-value="0">流式输出</el-checkbox>
          <el-checkbox v-model="modelForm.supportsTools" :true-value="1" :false-value="0">工具调用</el-checkbox>
          <el-checkbox v-model="modelForm.supportsVision" :true-value="1" :false-value="0">视觉输入</el-checkbox>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="modelForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="modelForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="apiKeyDialogVisible" :title="editingApiKeyId ? '编辑 API Key' : '新增 API Key'" width="680px">
      <el-form label-width="120px">
        <el-form-item label="供应商">
          <el-select v-model="apiKeyForm.providerId" filterable placeholder="请选择供应商">
            <el-option v-for="provider in enabledProviders" :key="provider.id" :label="provider.providerName" :value="provider.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="apiKeyForm.modelId" filterable placeholder="请选择模型">
            <el-option
              v-for="model in apiKeyModelOptions"
              :key="model.id"
              :label="model.displayName || model.modelName"
              :value="model.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Key 名称"><el-input v-model="apiKeyForm.keyName" /></el-form-item>
        <el-form-item label="Key 类型">
          <el-select v-model="apiKeyForm.keyType">
            <el-option label="免费" value="free" />
            <el-option label="付费" value="paid" />
            <el-option label="个人" value="personal" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="apiKeyForm.apiKey" type="password" show-password :placeholder="editingApiKeyId ? '留空表示不修改当前 Key' : '请输入 API Key'" />
        </el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="apiKeyForm.priority" :min="1" :max="9999" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="apiKeyForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="apiKeyForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="apiKeyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveApiKey">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.model-page {
  max-width: 1580px;
}

.page-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-heading p {
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 6px;
}

.page-heading h1 {
  color: #0f172a;
  font-size: 30px;
  letter-spacing: 0;
  margin: 0;
}

.model-panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 22px;
}

.provider-table {
  margin-top: 18px;
}

.resource-count {
  color: #334155;
  display: grid;
  gap: 4px;
}

.provider-detail-head {
  align-items: flex-start;
  display: flex;
  gap: 20px;
  justify-content: space-between;
  margin-bottom: 18px;
}

.provider-detail-head h2 {
  color: #111827;
  font-size: 24px;
  letter-spacing: 0;
  margin: 4px 0 6px;
}

.provider-detail-head span,
.drawer-section-title span,
.model-detail-header p,
.model-meta,
.model-note {
  color: #64748b;
}

.detail-eyebrow,
.drawer-section-title p {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  margin: 0;
}

.detail-actions,
.model-actions,
.model-title-row,
.model-meta,
.model-note {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.provider-descriptions {
  margin-bottom: 24px;
}

.drawer-section-title {
  margin-bottom: 14px;
}

.drawer-section-title p {
  color: #111827;
  font-size: 18px;
  margin-bottom: 4px;
}

.model-detail-list {
  display: grid;
  gap: 16px;
}

.model-detail-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
}

.model-detail-header {
  align-items: flex-start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 12px;
}

.model-title-row h3 {
  color: #111827;
  font-size: 18px;
  letter-spacing: 0;
  margin: 0;
}

.model-detail-header p {
  margin: 6px 0 0;
}

.model-meta,
.model-note {
  font-size: 13px;
  margin-bottom: 12px;
}

:deep(.el-select) {
  width: 100%;
}

@media (max-width: 900px) {
  .page-heading,
  .provider-detail-head,
  .model-detail-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
