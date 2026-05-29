<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import {
  createImageStorageConfig,
  listImageStorageConfigs,
  updateImageStorageConfig,
  validateImageStorageConfig,
  type ImageStorageConfig,
  type ImageStorageValidationResult,
  type ImageStoragePayload
} from "../../api/imageStorage";

type StorageType = "oss" | "cos" | "custom";

const loading = ref(false);
const configs = ref<ImageStorageConfig[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number>();
const form = reactive<ImageStoragePayload>(defaultForm("cos"));
const validating = ref(false);
const validatedSignature = ref("");
const validationResult = ref<ImageStorageValidationResult | null>(null);

const dialogTitle = computed(() => editingId.value ? "编辑图片存储" : "新增图片存储");
const isAliyunOss = computed(() => form.storageType === "oss");
const isTencentCos = computed(() => form.storageType === "cos");
const isCustomStorage = computed(() => form.storageType === "custom");
const secretIdLabel = computed(() => isTencentCos.value ? "SecretId" : "AccessKeyId");
const secretKeyLabel = computed(() => isTencentCos.value ? "SecretKey" : "AccessKeySecret");
const currentPayloadSignature = computed(() => payloadSignature(buildPayload()));
const validationPassed = computed(() => Boolean(validationResult.value?.success && validatedSignature.value === currentPayloadSignature.value));

function defaultForm(storageType: StorageType): ImageStoragePayload {
  if (storageType === "oss") {
    return {
      storageCode: "aliyun_oss",
      storageName: "阿里云 OSS",
      storageType: "oss",
      endpoint: "",
      region: "",
      bucketName: "",
      baseUrl: "",
      rootPath: "chat-images",
      extraConfigJson: "",
      accessKeyId: "",
      accessKeySecret: "",
      enabled: 1,
      isDefault: 1,
      remark: "用户上传图片保存到阿里云 OSS"
    };
  }

  if (storageType === "cos") {
    return {
      storageCode: "tencent_cos",
      storageName: "腾讯云 COS",
      storageType: "cos",
      endpoint: "",
      region: "",
      bucketName: "",
      baseUrl: "",
      rootPath: "chat-images",
      extraConfigJson: "",
      accessKeyId: "",
      accessKeySecret: "",
      enabled: 1,
      isDefault: 1,
      remark: "用户上传图片保存到腾讯云 COS"
    };
  }

  return {
    storageCode: "custom_storage",
    storageName: "其他对象存储",
    storageType: "custom",
    endpoint: "",
    region: "",
    bucketName: "",
    baseUrl: "",
    rootPath: "chat-images",
    extraConfigJson: "",
    accessKeyId: "",
    accessKeySecret: "",
    enabled: 1,
    isDefault: 0,
    remark: ""
  };
}

async function loadConfigs() {
  loading.value = true;
  try {
    configs.value = await listImageStorageConfigs();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = undefined;
  Object.assign(form, defaultForm("cos"));
  resetValidation();
  dialogVisible.value = true;
}

function openEdit(config: ImageStorageConfig) {
  editingId.value = config.id;
  Object.assign(form, {
    storageCode: config.storageCode,
    storageName: config.storageName,
    storageType: normalizeStorageType(config.storageType),
    endpoint: config.endpoint || "",
    region: config.region || "",
    bucketName: config.bucketName || "",
    baseUrl: config.baseUrl || "",
    rootPath: config.rootPath || "",
    extraConfigJson: config.extraConfigJson || "",
    accessKeyId: config.accessKeyId || "",
    accessKeySecret: config.accessKeySecret || "",
    enabled: config.enabled,
    isDefault: config.isDefault,
    remark: config.remark || ""
  });
  resetValidation();
  dialogVisible.value = true;
}

function handleStorageTypeChange(storageType: StorageType) {
  Object.assign(form, defaultForm(storageType));
  resetValidation();
}

async function validateConfig() {
  if (!validateFormFields()) {
    return;
  }

  const payload = buildPayload();
  validating.value = true;
  validationResult.value = null;
  validatedSignature.value = "";
  try {
    const result = await validateImageStorageConfig(payload, editingId.value);
    validationResult.value = result;
    if (result.success) {
      validatedSignature.value = payloadSignature(payload);
      ElMessage.success(result.message || "验证成功");
    } else {
      ElMessage.error(result.message || "验证失败");
    }
  } catch (error: any) {
    const message = error?.response?.data?.message || "验证失败";
    validationResult.value = { success: false, message, objectUrl: "" };
    ElMessage.error(message);
  } finally {
    validating.value = false;
  }
}

async function saveConfig() {
  if (!validateFormFields()) {
    return;
  }

  const payload = buildPayload();
  if (!validationResult.value?.success || validatedSignature.value !== payloadSignature(payload)) {
    return ElMessage.warning("请先验证配置，验证成功后才能保存");
  }

  try {
    if (editingId.value) {
      await updateImageStorageConfig(editingId.value, payload);
      ElMessage.success("图片存储配置已更新");
    } else {
      await createImageStorageConfig(payload);
      ElMessage.success("图片存储配置已创建");
    }
    dialogVisible.value = false;
    await loadConfigs();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "保存失败");
  }
}

function validateFormFields() {
  if (isAliyunOss.value || isTencentCos.value) {
    if (!form.region.trim() || !form.bucketName.trim()) {
      ElMessage.warning("请填写地域和 Bucket");
      return false;
    }
    if (!form.accessKeyId.trim() || !form.accessKeySecret.trim()) {
      ElMessage.warning("请填写访问密钥");
      return false;
    }
  }

  if (isCustomStorage.value && !form.rootPath.trim()) {
    ElMessage.warning("请填写保存前缀");
    return false;
  }
  if (isCustomStorage.value && (!form.storageName.trim() || !form.baseUrl.trim())) {
    ElMessage.warning("请填写存储名称和访问域名");
    return false;
  }

  return true;
}

function buildPayload(): ImageStoragePayload {
  const storageType = normalizeStorageType(form.storageType);
  const region = form.region.trim();
  const bucketName = form.bucketName.trim();
  const rootPath = form.rootPath.trim() || "chat-images";
  const payload: ImageStoragePayload = {
    ...form,
    storageType,
    region,
    bucketName,
    rootPath,
    enabled: 1,
    isDefault: 1,
    extraConfigJson: form.extraConfigJson.trim(),
    accessKeyId: form.accessKeyId.trim(),
    accessKeySecret: form.accessKeySecret.trim(),
    remark: form.remark.trim()
  };

  if (storageType === "oss") {
    payload.storageName = "阿里云 OSS";
    payload.storageCode = buildStorageCode("aliyun_oss", bucketName, region);
    payload.endpoint = buildAliyunEndpoint(region);
    payload.baseUrl = buildAliyunBaseUrl(bucketName, region);
    payload.remark = "用户上传图片保存到阿里云 OSS";
  } else if (storageType === "cos") {
    payload.storageName = "腾讯云 COS";
    payload.storageCode = buildStorageCode("tencent_cos", bucketName, region);
    payload.endpoint = "";
    payload.baseUrl = buildTencentBaseUrl(bucketName, region);
    payload.remark = "用户上传图片保存到腾讯云 COS";
  } else {
    payload.storageName = form.storageName.trim();
    payload.storageCode = buildStorageCode("custom_storage", bucketName || payload.storageName, region);
    payload.endpoint = form.endpoint.trim();
    payload.baseUrl = form.baseUrl.trim();
  }

  return payload;
}

function normalizeStorageType(type?: string): StorageType {
  if (type === "oss" || type === "cos" || type === "custom") {
    return type;
  }
  return "custom";
}

function buildStorageCode(prefix: string, name: string, region: string) {
  const source = [prefix, name, region]
    .filter(Boolean)
    .join("_")
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, "_")
    .replace(/^_+|_+$/g, "");
  return source || prefix;
}

function buildAliyunEndpoint(region: string) {
  return region ? `https://oss-${region}.aliyuncs.com` : "";
}

function buildAliyunBaseUrl(bucketName: string, region: string) {
  return bucketName && region ? `https://${bucketName}.oss-${region}.aliyuncs.com` : "";
}

function buildTencentBaseUrl(bucketName: string, region: string) {
  return bucketName && region ? `https://${bucketName}.cos.${region}.myqcloud.com` : "";
}

function payloadSignature(payload: ImageStoragePayload) {
  return JSON.stringify({
    storageType: payload.storageType,
    endpoint: payload.endpoint,
    region: payload.region,
    bucketName: payload.bucketName,
    baseUrl: payload.baseUrl,
    rootPath: payload.rootPath,
    accessKeyId: payload.accessKeyId,
    accessKeySecret: payload.accessKeySecret,
    extraConfigJson: payload.extraConfigJson
  });
}

function resetValidation() {
  validationResult.value = null;
  validatedSignature.value = "";
}

watch(form, () => {
  if (validationResult.value) {
    resetValidation();
  }
}, { deep: true });

function statusTag(enabled: number) {
  return enabled === 1 ? "success" : "info";
}

function storageTypeText(type?: string) {
  const map: Record<string, string> = {
    oss: "阿里云 OSS",
    cos: "腾讯云 COS",
    custom: "其他",
    object_storage: "对象存储",
    qiniu: "七牛云",
    s3: "S3",
    local: "本地存储"
  };
  return map[type || ""] || type || "-";
}

function storageTypeTag(type?: string) {
  if (type === "oss") return "success";
  if (type === "cos") return "primary";
  return "info";
}

function accessInfo(row: ImageStorageConfig) {
  return row.baseUrl || row.endpoint || row.rootPath || "-";
}

function bucketInfo(row: ImageStorageConfig) {
  if (row.bucketName && row.rootPath) {
    return `${row.bucketName} / ${row.rootPath}`;
  }
  return row.bucketName || row.rootPath || "-";
}

function displayText(value?: string | null) {
  return value && value.trim() ? value : "-";
}

onMounted(loadConfigs);
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p>Storage</p>
        <h1>图片存储管理</h1>
      </div>
      <div class="image-storage-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增</el-button>
      </div>
    </div>

    <el-card shadow="never" class="panel-card image-storage-card">
      <el-alert
        title="图片存储用于用户上传图片和后续可编辑素材"
        description="当前策略：AI 厂商生成或编辑返回的图片先直接展示厂商 URL；用户上传图片、用户确认保存或继续编辑的图片，再进入这里配置的默认存储。"
        type="info"
        show-icon
        :closable="false"
      />

      <el-table v-loading="loading" :data="configs" class="image-storage-table">
        <el-table-column prop="storageName" label="名称" min-width="160" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="storageTypeTag(row.storageType)">{{ storageTypeText(row.storageType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="访问域名 / Endpoint" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">{{ accessInfo(row) }}</template>
        </el-table-column>
        <el-table-column label="Bucket / 前缀" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ bucketInfo(row) }}</template>
        </el-table-column>
        <el-table-column label="密钥 ID" width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ displayText(row.accessKeyId) }}</template>
        </el-table-column>
        <el-table-column label="密钥 Secret" width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ displayText(row.accessKeySecret) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.enabled)">{{ row.enabled === 1 ? "启用" : "停用" }}</el-tag>
            <el-tag v-if="row.isDefault === 1" type="primary" class="default-tag">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ displayText(row.remark) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form label-width="126px">
        <el-alert
          class="validation-hint"
          title="验证配置会真实调用云存储"
          description="系统会上传一个很小的临时文件、访问公开 URL，并尝试删除临时文件；云厂商可能按请求次数计费。验证成功后才能保存。"
          type="warning"
          show-icon
          :closable="false"
        />

        <el-form-item label="存储方式">
          <el-select v-model="form.storageType" class="storage-type-select" @change="handleStorageTypeChange">
            <el-option label="阿里云 OSS" value="oss" />
            <el-option label="腾讯云 COS" value="cos" />
            <el-option label="其他" value="custom" />
          </el-select>
        </el-form-item>

        <template v-if="isCustomStorage">
          <el-form-item label="存储名称">
            <el-input v-model="form.storageName" placeholder="例如：自建对象存储" />
          </el-form-item>
          <el-form-item label="Endpoint">
            <el-input
              v-model="form.endpoint"
              placeholder="对象存储服务 Endpoint"
            />
          </el-form-item>
        </template>

        <el-form-item label="地域">
          <el-input
            v-model="form.region"
            :placeholder="isTencentCos ? 'ap-nanjing' : isAliyunOss ? 'cn-wuhan-lr' : '例如：ap-nanjing'"
          />
        </el-form-item>
        <el-form-item label="Bucket">
          <el-input
            v-model="form.bucketName"
            :placeholder="isTencentCos ? 'lantu-1308986692' : isAliyunOss ? 'lantu-boss-chat' : '存储桶名称'"
          />
        </el-form-item>
        <el-form-item v-if="isCustomStorage" label="访问域名">
          <el-input
            v-model="form.baseUrl"
            placeholder="公开访问域名或 CDN 域名"
          />
        </el-form-item>
        <el-form-item v-if="isCustomStorage" label="保存前缀">
          <el-input v-model="form.rootPath" placeholder="chat-images" />
        </el-form-item>

        <el-form-item :label="secretIdLabel">
          <el-input
            v-model="form.accessKeyId"
            placeholder="请输入密钥 ID"
          />
        </el-form-item>
        <el-form-item :label="secretKeyLabel">
          <el-input
            v-model="form.accessKeySecret"
            placeholder="请输入密钥 Secret"
          />
        </el-form-item>

        <el-form-item v-if="isCustomStorage" label="扩展配置">
          <el-input v-model="form.extraConfigJson" type="textarea" :rows="3" placeholder="可选 JSON，当前可留空" />
        </el-form-item>

        <el-alert
          v-if="validationResult"
          class="validation-result"
          :type="validationResult.success ? 'success' : 'error'"
          :title="validationResult.success ? '验证成功' : '验证失败'"
          :description="validationResult.message"
          show-icon
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="validating" @click="validateConfig">验证配置</el-button>
        <el-button type="primary" :disabled="!validationPassed" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.image-storage-actions {
  display: flex;
  gap: 10px;
}

.image-storage-table {
  margin-top: 18px;
}

.default-tag {
  margin-left: 6px;
}

.storage-type-select {
  width: 100%;
}

.validation-hint {
  margin-bottom: 18px;
}

.validation-result {
  margin-top: 8px;
}

.form-two-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
</style>
