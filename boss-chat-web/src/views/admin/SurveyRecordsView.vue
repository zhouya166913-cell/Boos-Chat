<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { CopyDocument, Download, Picture, Refresh } from "@element-plus/icons-vue";
import QRCode from "qrcode";
import {
  getSurveyRecord,
  listSurveyRecords,
  type SurveyRecordDetail,
  type SurveyRecordListItem
} from "../../api/surveyRecords";
import { getCourseDashboard, listCoursePhases, type CourseDashboard, type CoursePhase } from "../../api/coursePhases";

const records = ref<SurveyRecordListItem[]>([]);
const phases = ref<CoursePhase[]>([]);
const loading = ref(false);
const detailLoading = ref(false);
const keyword = ref("");
const selectedPhaseId = ref<number | undefined>();
const detailVisible = ref(false);
const qrDialogVisible = ref(false);
const dashboardDrawerVisible = ref(false);
const qrLoading = ref(false);
const qrImageUrl = ref("");
const currentDetail = ref<SurveyRecordDetail | null>(null);
const dashboard = ref<CourseDashboard | null>(null);

const selectedPhase = computed(() => phases.value.find((phase) => phase.id === selectedPhaseId.value));
const surveyUrl = computed(() => {
  const phase = selectedPhase.value;
  const baseUrl = import.meta.env.VITE_SURVEY_PUBLIC_URL || `${window.location.origin}/survey/enterprise-diagnosis.html`;
  if (!phase) return baseUrl;
  const url = new URL(baseUrl, window.location.origin);
  url.searchParams.set("phase", phase.phaseCode);
  return url.toString();
});

const filteredRecords = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  if (!term) return records.value;
  return records.value.filter((record) =>
    [record.customerName, record.phone, record.company, record.employeeCount, record.annualRevenue, record.status]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(term))
  );
});

const analyzerHtml = computed(() => renderReport(currentDetail.value?.analyzerResult || ""));
const finalReportHtml = computed(() =>
  renderReport(currentDetail.value?.finalReport || currentDetail.value?.errorMessage || "")
);

async function loadRecords() {
  loading.value = true;
  try {
    records.value = await listSurveyRecords(selectedPhaseId.value);
  } finally {
    loading.value = false;
  }
}

async function loadPhases() {
  phases.value = await listCoursePhases();
  if (!phases.value.length) {
    selectedPhaseId.value = undefined;
    return;
  }
  const selectedExists = phases.value.some((phase) => phase.id === selectedPhaseId.value);
  if (!selectedExists) {
    selectedPhaseId.value = phases.value[0].id;
  }
}

async function handlePhaseChange() {
  qrImageUrl.value = "";
  await loadRecords();
}

async function openDetail(record: SurveyRecordListItem) {
  detailLoading.value = true;
  detailVisible.value = true;
  try {
    currentDetail.value = await getSurveyRecord(record.publicId);
  } finally {
    detailLoading.value = false;
  }
}

async function copySurveyUrl() {
  await navigator.clipboard.writeText(surveyUrl.value);
  ElMessage.success("问卷链接已复制");
}

async function ensureSurveyQrImage() {
  if (qrImageUrl.value) return;
  qrLoading.value = true;
  try {
    qrImageUrl.value = await QRCode.toDataURL(surveyUrl.value, {
      errorCorrectionLevel: "M",
      margin: 2,
      width: 320,
      color: {
        dark: "#0f172a",
        light: "#ffffff"
      }
    });
  } finally {
    qrLoading.value = false;
  }
}

async function openQrDialog() {
  qrDialogVisible.value = true;
  try {
    await ensureSurveyQrImage();
  } catch (error) {
    ElMessage.error("二维码生成失败，请稍后重试");
  }
}

async function downloadSurveyQrImage() {
  await ensureSurveyQrImage();
  const link = document.createElement("a");
  link.href = qrImageUrl.value;
  link.download = "企业AI诊断问卷二维码.png";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

async function openDashboard() {
  if (!selectedPhaseId.value) {
    ElMessage.warning("请先选择一个期数");
    return;
  }
  dashboard.value = await getCourseDashboard(selectedPhaseId.value);
  dashboardDrawerVisible.value = true;
}

function statusType(status: string) {
  if (status === "COMPLETED") return "success";
  if (status === "FAILED") return "danger";
  if (status === "ANALYZING") return "warning";
  return "info";
}

function statusText(status: string) {
  const map: Record<string, string> = {
    PENDING: "待处理",
    ANALYZING: "生成中",
    COMPLETED: "已完成",
    FAILED: "失败"
  };
  return map[status] || status || "-";
}

function formatDate(value?: string) {
  return value ? value.replace("T", " ") : "-";
}

function formatAnswer(value: unknown) {
  if (Array.isArray(value)) return value.length ? value.join("、") : "未填写";
  return value ? String(value) : "未填写";
}

function escapeHtml(value: string) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function cleanInline(text: string) {
  return String(text || "")
    .replace(/\[\s*\]\s*/g, "")
    .replace(/`/g, "")
    .replace(/\*/g, "")
    .replace(/#{1,6}\s*/g, "")
    .replace(/\s+\|/g, " ")
    .replace(/\|\s+/g, " ")
    .replace(/\|/g, " ")
    .replace(/\s{2,}/g, " ")
    .trim();
}

function renderInline(text: string) {
  return escapeHtml(cleanInline(text))
    .replace(/【(痛点|需求|关键方案)(：|:)(.+?)】/g, '<strong class="ai-highlight">$1$2$3</strong>')
    .replace(/（(.+?)）/g, "<span class=\"muted-inline\">（$1）</span>");
}

function normalizeReport(text: string) {
  return String(text || "")
    .replace(/\r/g, "")
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => {
      if (!line) return true;
      if (/^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)+\|?$/.test(line)) return false;
      if (/^[-_*]{2,}$/.test(line)) return false;
      if (/^\|\s*$/.test(line)) return false;
      return true;
    })
    .map((line) =>
      line
        .replace(/^#{1,6}\s*/, "")
        .replace(/^\s*[>-]\s*/, "")
        .replace(/^\s*[-*]\s+(?=\S)/, "• ")
        .replace(/\[\s*\]\s*/g, "")
        .replace(/`/g, "")
        .replace(/\|/g, " ")
        .replace(/\s{2,}/g, " ")
        .trim()
    )
    .join("\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function isHeading(line: string) {
  const text = cleanInline(line);
  if (!text) return false;
  if (/^[一二三四五六七八九十]+[、.．]/.test(text)) return true;
  if (/^\d+[、.．]\s*\S{2,24}$/.test(text)) return true;
  return [
    "客户画像",
    "核心痛点",
    "AI落地成熟度",
    "销售跟进判断",
    "给规划AI的任务提示词",
    "诊断结论",
    "当前关键问题",
    "关键问题",
    "AI赋能切入点",
    "AI 赋能切入点",
    "落地建议",
    "90天落地建议",
    "90 天落地建议",
    "沟通与跟进建议",
    "总结建议",
    "风险提醒",
    "下一步行动"
  ].some((keyword) => text.includes(keyword) && text.length <= 32);
}

function titleText(line: string) {
  return cleanInline(line).replace(/^[一二三四五六七八九十\d]+[、.．]\s*/, "");
}

function renderReport(text: string) {
  const normalized = normalizeReport(text);
  if (!normalized) {
    return `<p class="survey-report-empty">暂无内容</p>`;
  }

  const lines = normalized.split("\n");
  const sections: Array<{ title: string; lines: string[] }> = [];
  let current = { title: "内容摘要", lines: [] as string[] };

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      current.lines.push("");
      continue;
    }
    if (isHeading(line)) {
      if (current.lines.some((item) => item.trim())) {
        sections.push(current);
      }
      current = { title: titleText(line), lines: [] };
    } else {
      current.lines.push(line);
    }
  }
  if (current.lines.some((item) => item.trim())) {
    sections.push(current);
  }

  return `<div class="survey-report-view">${sections.map(renderSection).join("")}</div>`;
}

function renderSection(section: { title: string; lines: string[] }) {
  const blocks: string[] = [];
  let listItems: string[] = [];
  let orderedItems: string[] = [];
  let paragraph: string[] = [];

  function flushParagraph() {
    if (!paragraph.length) return;
    blocks.push(`<p>${renderInline(paragraph.join(" "))}</p>`);
    paragraph = [];
  }

  function flushList() {
    if (listItems.length) {
      blocks.push(`<ul>${listItems.map((item) => `<li>${renderInline(item)}</li>`).join("")}</ul>`);
      listItems = [];
    }
    if (orderedItems.length) {
      blocks.push(`<ol>${orderedItems.map((item) => `<li>${renderInline(item)}</li>`).join("")}</ol>`);
      orderedItems = [];
    }
  }

  for (const rawLine of section.lines) {
    const line = rawLine.trim();
    if (!line) {
      flushParagraph();
      flushList();
      continue;
    }
    const unordered = line.match(/^•\s*(.+)$/);
    const numbered = line.match(/^\d+[、.．]\s*(.+)$/);
    if (unordered) {
      flushParagraph();
      orderedItems = [];
      listItems.push(unordered[1]);
      continue;
    }
    if (numbered) {
      flushParagraph();
      listItems = [];
      orderedItems.push(numbered[1]);
      continue;
    }
    if (/^[^：:]{2,18}[：:]/.test(line)) {
      flushParagraph();
      flushList();
      blocks.push(`<p>${renderInline(line)}</p>`);
      continue;
    }
    paragraph.push(line);
  }

  flushParagraph();
  flushList();

  return `<section class="survey-report-section"><h3>${renderInline(section.title)}</h3>${blocks.join("")}</section>`;
}

onMounted(async () => {
  await loadPhases();
  await loadRecords();
});
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p>Survey</p>
        <h1>调查记录</h1>
      </div>
      <div class="survey-page-actions">
        <el-select
          v-model="selectedPhaseId"
          clearable
          placeholder="选择期数"
          style="width: 260px"
          @change="handlePhaseChange"
          @clear="handlePhaseChange"
        >
          <el-option v-for="phase in phases" :key="phase.id" :label="phase.phaseName" :value="phase.id" />
        </el-select>
        <el-button @click="openDashboard">数据看板</el-button>
        <el-button :icon="CopyDocument" @click="copySurveyUrl">复制问卷链接</el-button>
        <el-button :icon="Picture" @click="openQrDialog">问卷二维码</el-button>
        <el-button type="primary" :icon="Refresh" @click="loadRecords">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="panel-card survey-records-card">
      <el-alert
        class="survey-records-alert"
        type="info"
        :closable="false"
        title="销售把对应期数的问卷链接发送到群里，客户提交后会自动归档到该期数"
        :description="surveyUrl"
      />

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名、电话、公司或状态" clearable />
      </div>

      <el-table v-loading="loading" :data="filteredRecords" class="survey-records-table">
        <el-table-column prop="phaseName" label="期数" min-width="180" />
        <el-table-column prop="customerName" label="客户姓名" min-width="120" />
        <el-table-column prop="company" label="公司" min-width="180" />
        <el-table-column prop="phone" label="电话" min-width="140" />
        <el-table-column prop="employeeCount" label="人数" min-width="110" />
        <el-table-column prop="annualRevenue" label="业绩" min-width="140" />
        <el-table-column label="AI 状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="qrDialogVisible" title="问卷二维码" width="420px" class="survey-qrcode-dialog" :lock-scroll="false">
      <div v-loading="qrLoading" class="survey-qrcode-panel">
        <div class="survey-qrcode-frame">
          <img v-if="qrImageUrl" :src="qrImageUrl" alt="企业 AI 诊断问卷二维码" />
        </div>
        <p class="survey-qrcode-title">企业 AI 落地诊断问卷</p>
        <p class="survey-qrcode-url">{{ surveyUrl }}</p>
        <div class="survey-qrcode-actions">
          <el-button type="primary" :icon="Download" @click="downloadSurveyQrImage">保存图片</el-button>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="调查详情" size="62%" :lock-scroll="false">
      <div v-loading="detailLoading">
        <template v-if="currentDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户姓名">{{ currentDetail.customerName }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ currentDetail.phone || "-" }}</el-descriptions-item>
            <el-descriptions-item label="公司">{{ currentDetail.company || "-" }}</el-descriptions-item>
            <el-descriptions-item label="公司人数">{{ currentDetail.employeeCount || "-" }}</el-descriptions-item>
            <el-descriptions-item label="公司业绩">{{ currentDetail.annualRevenue || "-" }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(currentDetail.status)">{{ statusText(currentDetail.status) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-card shadow="never" class="survey-detail-card">
            <template #header><strong>问卷答案</strong></template>
            <div class="survey-answer-list">
              <div v-for="(value, key) in currentDetail.answers" :key="key" class="survey-answer-item">
                <strong>{{ key }}</strong>
                <span>{{ formatAnswer(value) }}</span>
              </div>
            </div>
          </el-card>

          <el-card shadow="never" class="survey-detail-card">
            <template #header><strong>第一阶段 AI 分析</strong></template>
            <div class="survey-report-rendered" v-html="analyzerHtml"></div>
          </el-card>

          <el-card shadow="never" class="survey-detail-card">
            <template #header><strong>最终诊断方案</strong></template>
            <div class="survey-report-rendered" v-html="finalReportHtml"></div>
          </el-card>
        </template>
      </div>
    </el-drawer>

    <el-drawer v-model="dashboardDrawerVisible" title="数据看板" size="760px" :lock-scroll="false">
      <template v-if="dashboard">
        <div class="dashboard-grid">
          <div><strong>{{ dashboard.totalStudents }}</strong><span>学员</span></div>
          <div><strong>{{ dashboard.newStudentCount }}</strong><span>新学员</span></div>
          <div><strong>{{ dashboard.submittedCount }}</strong><span>已提交</span></div>
          <div><strong>{{ dashboard.missingSurveyCount }}</strong><span>未提交</span></div>
        </div>
        <h3>高频痛点</h3>
        <el-empty v-if="!dashboard.painPoints.length" description="暂无痛点数据" />
        <el-table v-else :data="dashboard.painPoints">
          <el-table-column prop="painPoint" label="痛点" />
          <el-table-column prop="count" label="出现次数" width="120" />
        </el-table>
        <h3>课程思路</h3>
        <ul class="idea-list">
          <li v-for="idea in dashboard.teachingIdeas" :key="idea">{{ idea }}</li>
        </ul>
        <h3>学员痛点摘要</h3>
        <el-table :data="dashboard.studentPainSummaries">
          <el-table-column prop="studentName" label="学员" width="120" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="痛点">
            <template #default="{ row }">{{ row.painPoints.join("、") || "未提取" }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.survey-page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.dashboard-grid div {
  display: grid;
  gap: 6px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.dashboard-grid strong {
  font-size: 28px;
  color: #1d4ed8;
}

.dashboard-grid span,
.idea-list {
  color: #64748b;
}

.idea-list {
  padding-left: 20px;
  line-height: 1.9;
}

.survey-records-alert {
  margin-bottom: 18px;
}

.survey-qrcode-panel {
  display: grid;
  justify-items: center;
  gap: 14px;
  min-height: 360px;
}

.survey-qrcode-frame {
  display: grid;
  place-items: center;
  width: 260px;
  height: 260px;
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.survey-qrcode-frame img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.survey-qrcode-title {
  margin: 0;
  color: #0f172a;
  font-weight: 800;
}

.survey-qrcode-url {
  width: 100%;
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
  overflow-wrap: anywhere;
}

.survey-qrcode-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.toolbar {
  max-width: 360px;
  margin-bottom: 14px;
}

.survey-detail-card {
  margin-top: 18px;
}

.survey-answer-list {
  display: grid;
  gap: 10px;
}

.survey-answer-item {
  display: grid;
  grid-template-columns: minmax(150px, 220px) minmax(0, 1fr);
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  line-height: 1.7;
}

.survey-answer-item strong {
  color: #0f172a;
}

.survey-answer-item span {
  color: #334155;
}

.survey-report-rendered :deep(.survey-report-view) {
  display: grid;
  gap: 14px;
}

.survey-report-rendered :deep(.survey-report-section) {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 14px;
  background: #fff;
}

.survey-report-rendered :deep(.survey-report-section:first-child) {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.survey-report-rendered :deep(h3) {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.4;
}

.survey-report-rendered :deep(p) {
  margin: 0 0 10px;
  color: #1f2937;
  line-height: 1.85;
  white-space: normal;
}

.survey-report-rendered :deep(p:last-child) {
  margin-bottom: 0;
}

.survey-report-rendered :deep(ul),
.survey-report-rendered :deep(ol) {
  margin: 0;
  padding-left: 22px;
}

.survey-report-rendered :deep(li) {
  margin: 8px 0;
  color: #1f2937;
  line-height: 1.75;
}

.survey-report-rendered :deep(strong) {
  color: #0f172a;
  font-weight: 800;
}

.survey-report-rendered :deep(.ai-highlight) {
  color: #020617;
  font-weight: 900;
}

.survey-report-rendered :deep(.muted-inline) {
  color: #64748b;
}

.survey-report-rendered :deep(.survey-report-empty) {
  color: #64748b;
}

@media (max-width: 900px) {
  .survey-answer-item {
    grid-template-columns: 1fr;
  }

  .survey-qrcode-actions {
    width: 100%;
  }

  .survey-qrcode-actions .el-button {
    flex: 1;
  }
}
</style>
