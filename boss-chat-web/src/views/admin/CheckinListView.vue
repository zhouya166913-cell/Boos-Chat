<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import QRCode from "qrcode";
import { ElMessage, ElMessageBox } from "element-plus";
import { Download, Refresh } from "@element-plus/icons-vue";
import {
  analyzeCoursePhase,
  createCoursePhase,
  createCourseStudent,
  deleteCourseStudent,
  getCourseDashboard,
  listCourseAnalyses,
  listCoursePhases,
  listCourseStudents,
  updateCoursePhase,
  updateCourseStudent,
  type CourseAnalysis,
  type CourseDashboard,
  type CoursePhase,
  type CourseStudent
} from "../../api/coursePhases";
import { listAdminAgents, type Agent } from "../../api/agents";
import {
  deleteSurveyRecord,
  deleteSurveyRecords,
  getSurveyRecord,
  listSurveyRecords,
  type SurveyRecordDetail,
  type SurveyRecordListItem
} from "../../api/surveyRecords";

const phases = ref<CoursePhase[]>([]);
const selectedPhaseId = ref<number | null>(null);
const students = ref<CourseStudent[]>([]);
const records = ref<SurveyRecordListItem[]>([]);
const dashboard = ref<CourseDashboard | null>(null);
const courseAnalysis = ref<CourseAnalysis | null>(null);
const courseAnalysisHistory = ref<CourseAnalysis[]>([]);
const analysisAgents = ref<Agent[]>([]);
const selectedAnalysisAgentId = ref<number | null>(null);
const selectedRecord = ref<SurveyRecordDetail | null>(null);

const loading = ref(false);
const studentsLoading = ref(false);
const recordsLoading = ref(false);
const analysisLoading = ref(false);
const phaseDialogVisible = ref(false);
const studentDialogVisible = ref(false);
const qrDialogVisible = ref(false);
const dashboardDrawerVisible = ref(false);
const detailDrawerVisible = ref(false);
const editingPhase = ref<CoursePhase | null>(null);
const editingStudent = ref<CourseStudent | null>(null);
const generatedQrImage = ref("");

const selectedPhase = computed(() => phases.value.find((phase) => phase.id === selectedPhaseId.value) || null);
const qrImage = computed(() => selectedPhase.value?.qrImageUrl || generatedQrImage.value);

const phaseForm = reactive({
  phaseName: "",
  courseName: "AI运营操盘手",
  qrImageUrl: "",
  enabled: 1,
  remark: ""
});

const studentForm = reactive({
  studentName: "",
  phone: "",
  idCard: "",
  isNewStudent: 1,
  remark: ""
});

function phaseSurveyUrl(phase: CoursePhase) {
  const baseUrl = import.meta.env.VITE_SURVEY_PUBLIC_URL
    || `${window.location.origin}${phase.surveyPath || "/survey/enterprise-diagnosis.html"}`;
  const url = new URL(baseUrl, window.location.origin);
  url.searchParams.set("phase", phase.phaseCode);
  return url.toString();
}

async function loadPhases() {
  loading.value = true;
  try {
    phases.value = await listCoursePhases();
    if (!phases.value.length) {
      selectedPhaseId.value = null;
      return;
    }
    if (!selectedPhaseId.value || !phases.value.some((phase) => phase.id === selectedPhaseId.value)) {
      selectedPhaseId.value = phases.value[0].id;
    }
  } finally {
    loading.value = false;
  }
}

async function loadCurrentPhaseData() {
  if (!selectedPhase.value) {
    students.value = [];
    records.value = [];
    dashboard.value = null;
    courseAnalysis.value = null;
    courseAnalysisHistory.value = [];
    return;
  }
  studentsLoading.value = true;
  recordsLoading.value = true;
  try {
    const phaseId = selectedPhase.value.id;
    const [studentRows, recordRows, dashboardData] = await Promise.all([
      listCourseStudents(phaseId),
      listSurveyRecords(phaseId),
      getCourseDashboard(phaseId)
    ]);
    students.value = studentRows;
    records.value = recordRows;
    dashboard.value = dashboardData;
    courseAnalysis.value = null;
    courseAnalysisHistory.value = [];
  } finally {
    studentsLoading.value = false;
    recordsLoading.value = false;
  }
}

async function refreshAll() {
  await loadPhases();
  await loadCurrentPhaseData();
}

function resetPhaseForm() {
  editingPhase.value = null;
  phaseForm.phaseName = "";
  phaseForm.courseName = "AI运营操盘手";
  phaseForm.qrImageUrl = "";
  phaseForm.enabled = 1;
  phaseForm.remark = "";
}

function openCreatePhase() {
  resetPhaseForm();
  phaseDialogVisible.value = true;
}

function openEditPhase(phase = selectedPhase.value) {
  if (!phase) return;
  editingPhase.value = phase;
  phaseForm.phaseName = phase.phaseName;
  phaseForm.courseName = phase.courseName || "AI运营操盘手";
  phaseForm.qrImageUrl = phase.qrImageUrl || "";
  phaseForm.enabled = phase.enabled;
  phaseForm.remark = phase.remark || "";
  phaseDialogVisible.value = true;
}

async function savePhase() {
  if (!phaseForm.phaseName.trim()) {
    ElMessage.warning("请填写期数名称");
    return;
  }
  const payload = {
    phaseName: phaseForm.phaseName.trim(),
    courseName: phaseForm.courseName.trim() || "AI运营操盘手",
    qrImageUrl: phaseForm.qrImageUrl.trim(),
    enabled: phaseForm.enabled,
    remark: phaseForm.remark.trim()
  };
  const saved = editingPhase.value
    ? await updateCoursePhase(editingPhase.value.id, payload)
    : await createCoursePhase(payload);
  ElMessage.success(editingPhase.value ? "期数已更新" : "期数已创建");
  phaseDialogVisible.value = false;
  selectedPhaseId.value = saved.id;
  await refreshAll();
}

function resetStudentForm() {
  editingStudent.value = null;
  studentForm.studentName = "";
  studentForm.phone = "";
  studentForm.idCard = "";
  studentForm.isNewStudent = 1;
  studentForm.remark = "";
}

function openCreateStudent() {
  if (!selectedPhase.value) {
    ElMessage.warning("请先创建或选择一期课程");
    return;
  }
  resetStudentForm();
  studentDialogVisible.value = true;
}

function openEditStudent(student: CourseStudent) {
  editingStudent.value = student;
  studentForm.studentName = student.studentName;
  studentForm.phone = student.phone || "";
  studentForm.idCard = student.idCard || "";
  studentForm.isNewStudent = student.isNewStudent;
  studentForm.remark = student.remark || "";
  studentDialogVisible.value = true;
}

async function saveStudent() {
  if (!selectedPhase.value) return;
  if (!studentForm.studentName.trim()) {
    ElMessage.warning("请填写姓名");
    return;
  }
  if (studentForm.phone.trim() && !/^1[3-9]\d{9}$/.test(studentForm.phone.trim())) {
    ElMessage.warning("手机号格式不正确");
    return;
  }
  const payload = {
    studentName: studentForm.studentName.trim(),
    phone: studentForm.phone.trim(),
    idCard: studentForm.idCard.trim().toUpperCase(),
    isNewStudent: studentForm.isNewStudent,
    remark: studentForm.remark.trim()
  };
  if (editingStudent.value) {
    await updateCourseStudent(selectedPhase.value.id, editingStudent.value.id, payload);
    ElMessage.success("学员已更新");
  } else {
    await createCourseStudent(selectedPhase.value.id, payload);
    ElMessage.success("学员已添加");
  }
  studentDialogVisible.value = false;
  await refreshAll();
}

async function removeStudent(student: CourseStudent) {
  if (!selectedPhase.value) return;
  await ElMessageBox.confirm(`确定删除学员“${student.studentName}”吗？`, "删除学员", { type: "warning" });
  await deleteCourseStudent(selectedPhase.value.id, student.id);
  await refreshAll();
  ElMessage.success("学员已删除");
}

async function openQr() {
  if (!selectedPhase.value) return;
  generatedQrImage.value = await QRCode.toDataURL(phaseSurveyUrl(selectedPhase.value), {
    width: 320,
    margin: 2,
    color: { dark: "#111827", light: "#ffffff" }
  });
  qrDialogVisible.value = true;
}

async function copySurveyUrl() {
  if (!selectedPhase.value) return;
  await navigator.clipboard.writeText(phaseSurveyUrl(selectedPhase.value));
  ElMessage.success("问卷链接已复制");
}

function downloadQrImage() {
  if (!selectedPhase.value || !qrImage.value) return;
  const link = document.createElement("a");
  link.href = qrImage.value;
  link.download = `${selectedPhase.value.phaseName}-问卷二维码.png`;
  link.click();
}

async function showDashboard() {
  if (!selectedPhase.value) return;
  dashboardDrawerVisible.value = true;
  await Promise.all([
    loadAnalysisAgents(),
    loadAnalysisHistory(),
    getCourseDashboard(selectedPhase.value.id).then((data) => {
      dashboard.value = data;
    })
  ]);
}

async function runCourseAnalysis() {
  if (!selectedPhase.value) return;
  analysisLoading.value = true;
  try {
    courseAnalysis.value = await analyzeCoursePhase(selectedPhase.value.id, selectedAnalysisAgentId.value);
    await loadAnalysisHistory();
    ElMessage.success("课程分析已生成");
  } finally {
    analysisLoading.value = false;
  }
}

async function loadAnalysisAgents() {
  if (analysisAgents.value.length) {
    return;
  }
  analysisAgents.value = (await listAdminAgents()).filter((agent) => agent.enabled === 1);
  if (!selectedAnalysisAgentId.value) {
    selectedAnalysisAgentId.value = analysisAgents.value.find((agent) => agent.agentCode === "survey_solution_planner")?.id
      ?? analysisAgents.value[0]?.id
      ?? null;
  }
}

async function loadAnalysisHistory() {
  if (!selectedPhase.value) {
    courseAnalysisHistory.value = [];
    return;
  }
  courseAnalysisHistory.value = await listCourseAnalyses(selectedPhase.value.id);
}

async function openRecordDetail(record: SurveyRecordListItem) {
  selectedRecord.value = await getSurveyRecord(record.publicId);
  detailDrawerVisible.value = true;
}

async function removeRecord(record: SurveyRecordListItem) {
  await ElMessageBox.confirm(`确定删除“${record.customerName}”的调查记录吗？`, "删除调查记录", { type: "warning" });
  await deleteSurveyRecord(record.publicId);
  await refreshAll();
  ElMessage.success("调查记录已删除");
}

async function removeAllRecords() {
  if (!selectedPhase.value) return;
  await ElMessageBox.confirm("确定删除当前期数的全部调查问卷记录吗？", "全部删除", { type: "warning" });
  await deleteSurveyRecords(selectedPhase.value.id);
  await refreshAll();
  ElMessage.success("当前期数调查记录已全部删除");
}

function statusLabel(status: string) {
  if (status === "COMPLETED") return "完成";
  if (status === "FAILED") return "失败";
  if (status === "ANALYZING") return "分析中";
  return status || "未知";
}

function statusType(status: string) {
  if (status === "COMPLETED") return "success";
  if (status === "FAILED") return "danger";
  if (status === "ANALYZING") return "warning";
  return "info";
}

function yesNo(value?: number) {
  return value === 1 ? "新学员" : "老学员";
}

watch(selectedPhaseId, () => {
  loadCurrentPhaseData();
});

onMounted(refreshAll);
</script>

<template>
  <section class="page-section">
    <div class="page-heading-row">
      <div>
        <p class="eyebrow">Course</p>
        <h1>课程管理</h1>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" @click="openCreatePhase">新增期数</el-button>
      </div>
    </div>

    <div class="table-card course-panel">
      <el-alert
        title="创建期数后会生成专属问卷链接和二维码。学员进入问卷时只按姓名匹配当前期数；手机号和身份证号可选，用于补充学员资料。"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="phase-toolbar">
        <el-select v-model="selectedPhaseId" placeholder="请选择课程期数" class="phase-select">
          <el-option v-for="phase in phases" :key="phase.id" :label="phase.phaseName" :value="phase.id" />
        </el-select>
        <el-button :disabled="!selectedPhase" @click="copySurveyUrl">复制问卷链接</el-button>
        <el-button :disabled="!selectedPhase" @click="openQr">二维码</el-button>
        <el-button :disabled="!selectedPhase" @click="openCreateStudent">新增学员</el-button>
        <el-button :disabled="!selectedPhase" @click="showDashboard">数据看板</el-button>
        <el-button :disabled="!selectedPhase" @click="openEditPhase()">编辑期数</el-button>
      </div>

      <div v-if="selectedPhase" class="survey-url">
        <span>{{ phaseSurveyUrl(selectedPhase) }}</span>
      </div>

      <div v-if="dashboard" class="stat-grid">
        <div class="stat-box"><strong>{{ dashboard.totalStudents }}</strong><span>学员</span></div>
        <div class="stat-box"><strong>{{ dashboard.newStudentCount }}</strong><span>新学员</span></div>
        <div class="stat-box"><strong>{{ dashboard.submittedCount }}</strong><span>已提交</span></div>
        <div class="stat-box"><strong>{{ dashboard.missingSurveyCount }}</strong><span>未提交</span></div>
      </div>
    </div>

    <div class="content-grid">
      <div class="table-card">
        <div class="card-header">
          <h2>学员名单</h2>
          <el-button size="small" type="primary" :disabled="!selectedPhase" @click="openCreateStudent">新增学员</el-button>
        </div>
        <el-table v-loading="studentsLoading" :data="students">
          <el-table-column prop="studentName" label="姓名" min-width="120" />
          <el-table-column prop="phone" label="手机号" min-width="140" />
          <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag :type="row.isNewStudent ? 'success' : 'info'">{{ yesNo(row.isNewStudent) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditStudent(row)">编辑</el-button>
              <el-button link type="danger" @click="removeStudent(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-card">
        <div class="card-header">
          <h2>调查问卷记录</h2>
          <div class="record-actions">
            <el-button size="small" :disabled="!selectedPhase || !records.length" @click="removeAllRecords">全部删除</el-button>
            <el-button size="small" :disabled="!selectedPhase" @click="showDashboard">数据看板</el-button>
          </div>
        </div>
        <el-table v-loading="recordsLoading" :data="records">
          <el-table-column prop="customerName" label="学员" min-width="110" />
          <el-table-column prop="phone" label="手机号" min-width="130" />
          <el-table-column prop="company" label="公司" min-width="150" show-overflow-tooltip />
          <el-table-column label="AI状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" min-width="170" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRecordDetail(row)">详情</el-button>
              <el-button link type="danger" @click="removeRecord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </section>

  <el-dialog v-model="phaseDialogVisible" :title="editingPhase ? '编辑期数' : '新增期数'" width="620px" :lock-scroll="false">
    <el-form label-width="110px">
      <el-form-item label="期数名称" required>
        <el-input v-model="phaseForm.phaseName" placeholder="例如：第十三期AI运营操盘手" />
      </el-form-item>
      <el-form-item label="课程名称">
        <el-input v-model="phaseForm.courseName" placeholder="默认：AI运营操盘手" />
      </el-form-item>
      <el-form-item label="替换二维码">
        <el-input v-model="phaseForm.qrImageUrl" placeholder="可选：粘贴图床二维码图片地址" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="phaseForm.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="phaseForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="phaseDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="savePhase">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="studentDialogVisible" :title="editingStudent ? '编辑学员' : '新增学员'" width="620px" :lock-scroll="false">
    <el-form label-width="110px">
      <el-form-item label="姓名" required>
        <el-input v-model="studentForm.studentName" placeholder="请输入学员姓名" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="studentForm.phone" maxlength="11" placeholder="可选填写11位手机号" />
      </el-form-item>
      <el-form-item label="身份证号">
        <el-input v-model="studentForm.idCard" maxlength="32" placeholder="可选填写身份证号" />
      </el-form-item>
      <el-form-item label="是否新学员">
        <el-switch v-model="studentForm.isNewStudent" :active-value="1" :inactive-value="0" active-text="新学员" inactive-text="老学员" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="studentForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="studentDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveStudent">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="qrDialogVisible" title="问卷二维码" width="460px" :lock-scroll="false">
    <div v-if="selectedPhase" class="qr-box">
      <img :src="qrImage" :alt="selectedPhase.phaseName" />
      <strong>{{ selectedPhase.phaseName }}</strong>
      <span>{{ phaseSurveyUrl(selectedPhase) }}</span>
      <el-button type="primary" :icon="Download" @click="downloadQrImage">保存图片</el-button>
    </div>
  </el-dialog>

  <el-drawer v-model="dashboardDrawerVisible" title="数据看板" size="58%" :lock-scroll="false">
    <div v-if="dashboard" class="dashboard-content">
      <div class="stat-grid">
        <div class="stat-box"><strong>{{ dashboard.totalStudents }}</strong><span>学员</span></div>
        <div class="stat-box"><strong>{{ dashboard.newStudentCount }}</strong><span>新学员</span></div>
        <div class="stat-box"><strong>{{ dashboard.completedCount }}</strong><span>已完成</span></div>
        <div class="stat-box"><strong>{{ dashboard.missingSurveyCount }}</strong><span>未提交</span></div>
      </div>

      <div class="card-header">
        <h2>高频痛点</h2>
      </div>
      <el-empty v-if="!dashboard.painPoints.length" description="暂无痛点数据" />
      <div v-else class="pain-list">
        <div v-for="item in dashboard.painPoints" :key="item.painPoint" class="pain-item">
          <span>{{ item.painPoint }}</span>
          <el-tag>{{ item.count }} 次</el-tag>
        </div>
      </div>

      <div class="analysis-toolbar">
        <el-select v-model="selectedAnalysisAgentId" placeholder="选择分析模型" filterable>
          <el-option
            v-for="agent in analysisAgents"
            :key="agent.id"
            :label="`${agent.agentName} / ${agent.modelName || '未绑定模型'}`"
            :value="agent.id"
          />
        </el-select>
        <el-button type="primary" :loading="analysisLoading" @click="runCourseAnalysis">课程分析</el-button>
      </div>

      <div v-if="courseAnalysis" class="analysis-box">
        <h2>本次分析结果</h2>
        <p>{{ courseAnalysis.agentName || "课程分析" }} · {{ courseAnalysis.generatedAt || "" }}</p>
        <pre>{{ courseAnalysis.content }}</pre>
      </div>

      <div class="analysis-box">
        <h2>历史分析结果</h2>
        <el-empty v-if="!courseAnalysisHistory.length" description="暂无历史分析" />
        <el-collapse v-else>
          <el-collapse-item
            v-for="item in courseAnalysisHistory"
            :key="item.id"
            :title="`${item.agentName || '课程分析'} · ${item.generatedAt || ''}`"
          >
            <pre>{{ item.content }}</pre>
          </el-collapse-item>
        </el-collapse>
      </div>

      <h2>学员痛点摘要</h2>
      <el-table :data="dashboard.studentPainSummaries">
        <el-table-column prop="studentName" label="学员" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="痛点">
          <template #default="{ row }">{{ row.painPoints?.join("、") || "暂无" }}</template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>

  <el-drawer v-model="detailDrawerVisible" title="调查问卷详情" size="58%" :lock-scroll="false">
    <div v-if="selectedRecord" class="detail-content">
      <h2>{{ selectedRecord.customerName }}</h2>
      <p>{{ selectedRecord.phaseName || "未归属期数" }} · {{ selectedRecord.phone }} · {{ selectedRecord.company || "未填写公司" }}</p>
      <el-tag :type="statusType(selectedRecord.status)">{{ statusLabel(selectedRecord.status) }}</el-tag>
      <h3>核心诊断报告</h3>
      <pre>{{ selectedRecord.finalReport || selectedRecord.errorMessage || "暂无报告" }}</pre>
    </div>
  </el-drawer>
</template>

<style scoped>
.heading-actions,
.phase-toolbar,
.card-header,
.record-actions,
.analysis-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.heading-actions {
  justify-content: flex-end;
}

.course-panel {
  margin-bottom: 18px;
}

.phase-toolbar {
  flex-wrap: wrap;
  margin-top: 18px;
}

.phase-select {
  width: 320px;
}

.survey-url {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #475569;
  background: #f8fafc;
  word-break: break-all;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.stat-box {
  padding: 14px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #f8fbff;
}

.stat-box strong {
  display: block;
  color: #2563eb;
  font-size: 28px;
  line-height: 1.1;
}

.stat-box span {
  color: #64748b;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 18px;
}

.card-header {
  justify-content: space-between;
  margin-bottom: 12px;
}

.record-actions {
  justify-content: flex-end;
}

.analysis-toolbar {
  justify-content: flex-start;
}

.analysis-toolbar .el-select {
  width: 300px;
}

.card-header h2,
.dashboard-content h2 {
  margin: 0;
  font-size: 18px;
}

.qr-box {
  display: grid;
  justify-items: center;
  gap: 14px;
  text-align: center;
}

.qr-box img {
  width: 260px;
  height: 260px;
  border: 1px solid #dbe4f0;
  border-radius: 16px;
  padding: 14px;
  background: #fff;
}

.qr-box span {
  max-width: 100%;
  color: #64748b;
  word-break: break-all;
}

.dashboard-content,
.detail-content {
  display: grid;
  gap: 18px;
}

.pain-list {
  display: grid;
  gap: 10px;
}

.pain-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

.analysis-box,
.detail-content pre {
  padding: 16px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #f8fafc;
}

.analysis-box p {
  margin: 4px 0 12px;
  color: #64748b;
}

.analysis-box pre,
.detail-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .content-grid,
  .stat-grid {
    grid-template-columns: 1fr;
  }

  .phase-select {
    width: 100%;
  }
}
</style>
