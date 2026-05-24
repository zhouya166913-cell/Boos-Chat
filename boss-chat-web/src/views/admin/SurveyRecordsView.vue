<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  getSurveyRecord,
  listSurveyRecords,
  type SurveyRecordDetail,
  type SurveyRecordListItem
} from "../../api/surveyRecords";

const records = ref<SurveyRecordListItem[]>([]);
const loading = ref(false);
const detailLoading = ref(false);
const keyword = ref("");
const detailVisible = ref(false);
const currentDetail = ref<SurveyRecordDetail | null>(null);

const surveyUrl = `${window.location.origin}/survey/enterprise-diagnosis.html`;

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
    records.value = await listSurveyRecords();
  } finally {
    loading.value = false;
  }
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
  await navigator.clipboard.writeText(surveyUrl);
  ElMessage.success("问卷链接已复制");
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

onMounted(loadRecords);
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p>Survey</p>
        <h1>调查记录</h1>
      </div>
      <div class="survey-page-actions">
        <el-button @click="copySurveyUrl">复制问卷链接</el-button>
        <el-button type="primary" @click="loadRecords">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="panel-card survey-records-card">
      <el-alert
        class="survey-records-alert"
        type="info"
        :closable="false"
        title="销售把固定问卷链接发送到群里，客户提交后会自动生成 AI 诊断记录"
        :description="surveyUrl"
      />

      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索姓名、电话、公司或状态" clearable />
      </div>

      <el-table v-loading="loading" :data="filteredRecords" class="survey-records-table">
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

    <el-drawer v-model="detailVisible" title="调查详情" size="62%">
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
  </section>
</template>

<style scoped>
.survey-page-actions {
  display: flex;
  gap: 10px;
}

.survey-records-alert {
  margin-bottom: 18px;
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
}
</style>
