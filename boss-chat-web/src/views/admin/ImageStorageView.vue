<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  createImageStorageConfig,
  listImageStorageConfigs,
  updateImageStorageConfig,
  type ImageStorageConfig,
  type ImageStoragePayload
} from "../../api/imageStorage";

const loading = ref(false);
const configs = ref<ImageStorageConfig[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number>();
const form = reactive<ImageStoragePayload>(defaultForm("local"));

const dialogTitle = computed(() => {
  const action = editingId.value ? "编辑" : "新增";
  const typeName = form.storageType === "oss" ? "阿里云 OSS" : form.storageType === "cos" ? "腾讯云 COS" : "本地存储";
  return `${action}${typeName}`;
});

function defaultForm(storageType: "local" | "oss" | "cos"): ImageStoragePayload {
  if (storageType === "oss") {
    return {
      storageCode: "aliyun_oss",
      storageName: "阿里云 OSS",
      storageType: "oss",
      endpoint: "https://oss-cn-wuhan-lr.aliyuncs.com",
      region: "cn-wuhan-lr",
      bucketName: "lantu-boss-chat",
      baseUrl: "https://lantu-boss-chat.oss-cn-wuhan-lr.aliyuncs.com",
      rootPath: "chat-images",
      extraConfigJson: "",
      accessKeyId: "",
      accessKeySecret: "",
      enabled: 1,
      isDefault: 1,
      remark: "用户上传图片优先保存到阿里云 OSS"
    };
  }
  if (storageType === "cos") {
    return {
      storageCode: "tencent_cos",
      storageName: "腾讯云 COS",
      storageType: "cos",
      endpoint: "",
      region: "ap-guangzhou",
      bucketName: "lantu-boss-chat-1314624174",
      baseUrl: "https://lantu-boss-chat-1314624174.cos.ap-guangzhou.myqcloud.com",
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
    storageCode: "local_dev",
    storageName: "本地开发存储",
    storageType: "local",
    endpoint: "",
    region: "",
    bucketName: "",
    baseUrl: "",
    rootPath: "uploads/chat-attachments",
    extraConfigJson: "",
    accessKeyId: "",
    accessKeySecret: "",
    enabled: 1,
    isDefault: 0,
    remark: "OSS 未启用时的本地兜底存储"
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

function openCreate(storageType: "local" | "oss" | "cos") {
  editingId.value = undefined;
  Object.assign(form, defaultForm(storageType));
  dialogVisible.value = true;
}

function openEdit(config: ImageStorageConfig) {
  editingId.value = config.id;
  Object.assign(form, {
    storageCode: config.storageCode,
    storageName: config.storageName,
    storageType: config.storageType || "local",
    endpoint: config.endpoint || "",
    region: config.region || "",
    bucketName: config.bucketName || "",
    baseUrl: config.baseUrl || "",
    rootPath: config.rootPath || "",
    extraConfigJson: config.extraConfigJson || "",
    accessKeyId: "",
    accessKeySecret: "",
    enabled: config.enabled,
    isDefault: config.isDefault,
    remark: config.remark || ""
  });
  dialogVisible.value = true;
}

async function saveConfig() {
  if (!form.storageCode.trim() || !form.storageName.trim()) {
    return ElMessage.warning("请填写存储编码和名称");
  }
  if (form.storageType === "local" && !form.rootPath.trim()) {
    return ElMessage.warning("请填写本地保存目录");
  }
  if (form.storageType === "oss" || form.storageType === "cos") {
    if (form.storageType === "oss" && !form.endpoint.trim()) {
      return ElMessage.warning("请填写 OSS Endpoint");
    }
    if (!form.region.trim() || !form.bucketName.trim() || !form.baseUrl.trim() || !form.rootPath.trim()) {
      return ElMessage.warning("请填写地域、Bucket、访问域名和保存前缀");
    }
    if (!editingId.value && (!form.accessKeyId.trim() || !form.accessKeySecret.trim())) {
      return ElMessage.warning("新增云存储配置时必须填写访问密钥");
    }
  }

  try {
    if (editingId.value) {
      await updateImageStorageConfig(editingId.value, form);
      ElMessage.success("图片存储配置已更新");
    } else {
      await createImageStorageConfig(form);
      ElMessage.success("图片存储配置已创建");
    }
    dialogVisible.value = false;
    await loadConfigs();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "保存失败");
  }
}

function statusTag(enabled: number) {
  return enabled === 1 ? "success" : "info";
}

function storageTypeText(type?: string) {
  const map: Record<string, string> = {
    local: "本地存储",
    oss: "阿里云 OSS",
    cos: "腾讯云 COS",
    object_storage: "对象存储",
    qiniu: "七牛云",
    s3: "S3",
    custom: "自定义"
  };
  return map[type || ""] || type || "-";
}

function storageTypeTag(type?: string) {
  if (type === "oss") return "success";
  if (type === "local") return "info";
  return "primary";
}

function accessInfo(row: ImageStorageConfig) {
  if (row.storageType === "oss") {
    return row.baseUrl || row.endpoint || "-";
  }
  return row.rootPath || "-";
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
        <el-button @click="openCreate('local')">新增本地存储</el-button>
        <el-button @click="openCreate('cos')">新增腾讯云 COS</el-button>
        <el-button type="primary" @click="openCreate('oss')">新增阿里云 OSS</el-button>
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
        <el-table-column prop="storageCode" label="编码" min-width="140" />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="storageTypeTag(row.storageType)">{{ storageTypeText(row.storageType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="访问域名 / 保存目录" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">{{ accessInfo(row) }}</template>
        </el-table-column>
        <el-table-column label="Bucket / 前缀" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.storageType === "oss" ? `${displayText(row.bucketName)} / ${displayText(row.rootPath)}` : displayText(row.rootPath) }}
          </template>
        </el-table-column>
        <el-table-column label="密钥" width="170">
          <template #default="{ row }">
            <span v-if="row.storageType === 'oss'">{{ row.accessKeyIdMask || "未配置" }}</span>
            <span v-else>-</span>
          </template>
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
        <el-form-item label="存储类型">
          <el-radio-group v-model="form.storageType" :disabled="Boolean(editingId)">
            <el-radio-button label="local">本地存储</el-radio-button>
            <el-radio-button label="oss">阿里云 OSS</el-radio-button>
            <el-radio-button label="cos">腾讯云 COS</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="存储编码">
          <el-input v-model="form.storageCode" placeholder="例如：aliyun_oss" />
        </el-form-item>
        <el-form-item label="存储名称">
          <el-input v-model="form.storageName" placeholder="例如：阿里云 OSS" />
        </el-form-item>

        <template v-if="form.storageType === 'oss' || form.storageType === 'cos'">
          <el-form-item v-if="form.storageType === 'oss'" label="Endpoint">
            <el-input v-model="form.endpoint" placeholder="https://oss-cn-wuhan-lr.aliyuncs.com" />
          </el-form-item>
          <el-form-item label="地域">
            <el-input v-model="form.region" :placeholder="form.storageType === 'cos' ? 'ap-guangzhou' : 'cn-wuhan-lr'" />
          </el-form-item>
          <el-form-item label="Bucket">
            <el-input v-model="form.bucketName" :placeholder="form.storageType === 'cos' ? 'lantu-boss-chat-1314624174' : 'lantu-boss-chat'" />
          </el-form-item>
          <el-form-item label="访问域名">
            <el-input
              v-model="form.baseUrl"
              :placeholder="form.storageType === 'cos'
                ? 'https://lantu-boss-chat-1314624174.cos.ap-guangzhou.myqcloud.com'
                : 'https://lantu-boss-chat.oss-cn-wuhan-lr.aliyuncs.com'"
            />
          </el-form-item>
          <el-form-item label="保存前缀">
            <el-input v-model="form.rootPath" placeholder="chat-images" />
          </el-form-item>
          <el-form-item :label="form.storageType === 'cos' ? 'SecretId' : 'AccessKeyId'">
            <el-input
              v-model="form.accessKeyId"
              type="password"
              show-password
              :placeholder="editingId ? '留空表示保留旧密钥 ID' : '请输入密钥 ID'"
            />
          </el-form-item>
          <el-form-item :label="form.storageType === 'cos' ? 'SecretKey' : 'AccessKeySecret'">
            <el-input
              v-model="form.accessKeySecret"
              type="password"
              show-password
              :placeholder="editingId ? '留空表示保留旧密钥 Secret' : '请输入密钥 Secret'"
            />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="保存目录">
            <el-input v-model="form.rootPath" placeholder="例如：uploads/chat-attachments" />
          </el-form-item>
        </template>

        <div class="form-two-columns">
          <el-form-item label="状态">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="默认存储">
            <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>
        <el-form-item label="扩展配置">
          <el-input v-model="form.extraConfigJson" type="textarea" :rows="3" placeholder="可选 JSON，当前可留空" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
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

.form-two-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
</style>
