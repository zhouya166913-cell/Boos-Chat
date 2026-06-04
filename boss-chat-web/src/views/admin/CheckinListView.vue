<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import QRCode from "qrcode";
import { ElMessage, ElMessageBox } from "element-plus";
import { Download, Refresh } from "@element-plus/icons-vue";
import {
  createCourseGroup,
  createCoursePhase,
  createCourseStudent,
  deleteCourseGroup,
  deleteCourseStudent,
  getCourseDashboard,
  listCourseAnalyses,
  listCourseGroups,
  listCoursePhases,
  listCourseStudents,
  streamCourseAnalysis,
  updateCourseGroup,
  updateCoursePhase,
  updateCourseStudent,
  type CourseAnalysis,
  type CourseDashboard,
  type CourseGroup,
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
const groups = ref<CourseGroup[]>([]);
const selectedPhaseId = ref<number | null>(null);
const selectedGroupId = ref<number | null>(null);
const students = ref<CourseStudent[]>([]);
const records = ref<SurveyRecordListItem[]>([]);
const dashboard = ref<CourseDashboard | null>(null);
const courseAnalysis = ref<CourseAnalysis | null>(null);
const courseAnalysisHistory = ref<CourseAnalysis[]>([]);
const analysisAgents = ref<Agent[]>([]);
const selectedAnalysisAgentId = ref<number | null>(null);
const selectedRecord = ref<SurveyRecordDetail | null>(null);

const loading = ref(false);
const groupsLoading = ref(false);
const studentsLoading = ref(false);
const recordsLoading = ref(false);
const analysisLoading = ref(false);
const phaseDialogVisible = ref(false);
const groupDialogVisible = ref(false);
const studentDialogVisible = ref(false);
const qrDialogVisible = ref(false);
const dashboardDrawerVisible = ref(false);
const detailDrawerVisible = ref(false);
const editingPhase = ref<CoursePhase | null>(null);
const editingGroup = ref<CourseGroup | null>(null);
const editingStudent = ref<CourseStudent | null>(null);
const generatedQrImage = ref("");

const selectedPhase = computed(() => phases.value.find((phase) => phase.id === selectedPhaseId.value) || null);
const selectedGroup = computed(() => groups.value.find((group) => group.id === selectedGroupId.value) || null);
const filteredStudents = computed(() => {
  if (!selectedGroupId.value) return students.value;
  return students.value.filter((student) => student.groupId === selectedGroupId.value);
});
const checkedInStudentCount = computed(() => students.value.filter((student) => (student.checkInCount || 0) > 0).length);
const qrImage = computed(() => selectedPhase.value?.qrImageUrl || generatedQrImage.value);
let checkInSnapshotTimer: number | null = null;

const phaseForm = reactive({
  phaseName: "",
  courseName: "AI运营操盘手",
  qrImageUrl: "",
  enabled: 1,
  remark: ""
});

const groupForm = reactive({
  groupName: "",
  leaderName: "",
  sortOrder: 0,
  remark: ""
});

const studentForm = reactive({
  groupId: null as number | null,
  studentName: "",
  phone: "",
  idCard: "",
  inviter: "",
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
    groups.value = [];
    selectedGroupId.value = null;
    students.value = [];
    records.value = [];
    dashboard.value = null;
    courseAnalysis.value = null;
    courseAnalysisHistory.value = [];
    return;
  }
  groupsLoading.value = true;
  studentsLoading.value = true;
  recordsLoading.value = true;
  try {
    const phaseId = selectedPhase.value.id;
    const [groupRows, studentRows, recordRows, dashboardData] = await Promise.all([
      listCourseGroups(phaseId),
      listCourseStudents(phaseId),
      listSurveyRecords(phaseId),
      getCourseDashboard(phaseId)
    ]);
    if (selectedPhase.value?.id !== phaseId) return;
    groups.value = groupRows;
    students.value = studentRows;
    records.value = recordRows;
    dashboard.value = dashboardData;
    courseAnalysis.value = null;
    courseAnalysisHistory.value = [];
    if (!groups.value.length) {
      selectedGroupId.value = null;
    } else if (!selectedGroupId.value || !groups.value.some((group) => group.id === selectedGroupId.value)) {
      selectedGroupId.value = groups.value[0].id;
    }
  } finally {
    groupsLoading.value = false;
    studentsLoading.value = false;
    recordsLoading.value = false;
  }
}

async function refreshCheckInSnapshot() {
  if (!selectedPhase.value || phaseDialogVisible.value || groupDialogVisible.value || studentDialogVisible.value) {
    return;
  }
  const phaseId = selectedPhase.value.id;
  const [groupRows, studentRows, dashboardData] = await Promise.all([
    listCourseGroups(phaseId),
    listCourseStudents(phaseId),
    getCourseDashboard(phaseId)
  ]);
  if (selectedPhase.value?.id !== phaseId) {
    return;
  }
  groups.value = groupRows;
  students.value = studentRows;
  dashboard.value = dashboardData;
  if (groups.value.length && (!selectedGroupId.value || !groups.value.some((group) => group.id === selectedGroupId.value))) {
    selectedGroupId.value = groups.value[0].id;
  }
}

function startCheckInSnapshotTimer() {
  stopCheckInSnapshotTimer();
  checkInSnapshotTimer = window.setInterval(() => {
    refreshCheckInSnapshot().catch(() => undefined);
  }, 5000);
}

function stopCheckInSnapshotTimer() {
  if (checkInSnapshotTimer !== null) {
    window.clearInterval(checkInSnapshotTimer);
    checkInSnapshotTimer = null;
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

function resetGroupForm() {
  editingGroup.value = null;
  groupForm.groupName = "";
  groupForm.leaderName = "";
  groupForm.sortOrder = groups.value.length + 1;
  groupForm.remark = "";
}

function openCreateGroup() {
  if (!selectedPhase.value) {
    ElMessage.warning("请先创建或选择一期课程");
    return;
  }
  resetGroupForm();
  groupDialogVisible.value = true;
}

function openEditGroup(group: CourseGroup) {
  editingGroup.value = group;
  groupForm.groupName = group.groupName;
  groupForm.leaderName = group.leaderName || "";
  groupForm.sortOrder = group.sortOrder || 0;
  groupForm.remark = group.remark || "";
  groupDialogVisible.value = true;
}

async function saveGroup() {
  if (!selectedPhase.value) return;
  if (!groupForm.groupName.trim()) {
    ElMessage.warning("请填写分组名称");
    return;
  }
  if (!groupForm.leaderName.trim()) {
    ElMessage.warning("请填写组长");
    return;
  }
  const payload = {
    groupName: groupForm.groupName.trim(),
    leaderName: groupForm.leaderName.trim(),
    sortOrder: groupForm.sortOrder,
    remark: groupForm.remark.trim()
  };
  const saved = editingGroup.value
    ? await updateCourseGroup(selectedPhase.value.id, editingGroup.value.id, payload)
    : await createCourseGroup(selectedPhase.value.id, payload);
  ElMessage.success(editingGroup.value ? "分组已更新" : "分组已创建");
  groupDialogVisible.value = false;
  selectedGroupId.value = saved.id;
  await refreshAll();
}

async function removeGroup(group: CourseGroup) {
  if (!selectedPhase.value) return;
  await ElMessageBox.confirm(`确定删除分组“${group.groupName}”吗？已有学员的分组不能删除。`, "删除分组", { type: "warning" });
  await deleteCourseGroup(selectedPhase.value.id, group.id);
  if (selectedGroupId.value === group.id) {
    selectedGroupId.value = null;
  }
  await refreshAll();
  ElMessage.success("分组已删除");
}

function selectGroup(group: CourseGroup) {
  selectedGroupId.value = group.id;
}

function groupStudentCount(group: CourseGroup) {
  return students.value.filter((student) => student.groupId === group.id).length || group.studentCount || 0;
}

function resetStudentForm() {
  editingStudent.value = null;
  studentForm.groupId = selectedGroupId.value || groups.value[0]?.id || null;
  studentForm.studentName = "";
  studentForm.phone = "";
  studentForm.idCard = "";
  studentForm.inviter = "";
  studentForm.isNewStudent = 1;
  studentForm.remark = "";
}

function openCreateStudent(group = selectedGroup.value) {
  if (!selectedPhase.value) {
    ElMessage.warning("请先创建或选择一期课程");
    return;
  }
  if (!groups.value.length) {
    ElMessage.warning("请先创建分组，再添加学员");
    return;
  }
  resetStudentForm();
  studentForm.groupId = group?.id || studentForm.groupId;
  studentDialogVisible.value = true;
}

function openEditStudent(student: CourseStudent) {
  editingStudent.value = student;
  studentForm.groupId = student.groupId || selectedGroupId.value || null;
  studentForm.studentName = student.studentName;
  studentForm.phone = student.phone || "";
  studentForm.idCard = student.idCard || "";
  studentForm.inviter = student.inviter || "";
  studentForm.isNewStudent = student.isNewStudent;
  studentForm.remark = student.remark || "";
  studentDialogVisible.value = true;
}

async function saveStudent() {
  if (!selectedPhase.value) return;
  if (!studentForm.groupId) {
    ElMessage.warning("请选择分组");
    return;
  }
  if (!studentForm.studentName.trim()) {
    ElMessage.warning("请填写姓名");
    return;
  }
  if (studentForm.phone.trim() && !/^1[3-9]\d{9}$/.test(studentForm.phone.trim())) {
    ElMessage.warning("手机号格式不正确");
    return;
  }
  const payload = {
    groupId: studentForm.groupId,
    seatNo: "",
    studentName: studentForm.studentName.trim(),
    phone: studentForm.phone.trim(),
    idCard: studentForm.idCard.trim().toUpperCase(),
    inviter: studentForm.inviter.trim(),
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
  selectedGroupId.value = studentForm.groupId;
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
  const agent = analysisAgents.value.find((item) => item.id === selectedAnalysisAgentId.value);
  courseAnalysis.value = {
    id: 0,
    phaseId: selectedPhase.value.id,
    phaseName: selectedPhase.value.phaseName,
    agentId: selectedAnalysisAgentId.value,
    agentName: agent?.agentName || "课程分析",
    content: "",
    generatedAt: "生成中"
  };
  try {
    await streamCourseAnalysis(selectedPhase.value.id, selectedAnalysisAgentId.value, {
      onDelta: (content) => {
        if (!courseAnalysis.value) return;
        courseAnalysis.value = {
          ...courseAnalysis.value,
          content: `${courseAnalysis.value.content || ""}${content}`
        };
      },
      onDone: (response) => {
        courseAnalysis.value = response;
      }
    });
    await loadAnalysisHistory();
    ElMessage.success("课程分析已生成");
  } catch (error) {
    courseAnalysis.value = null;
    ElMessage.error(error instanceof Error ? error.message : "课程分析失败");
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

function painText(row: { painPoints?: string[] }) {
  return row.painPoints?.join("、") || "暂无";
}

function shouldShowPainDetail(row: { painPoints?: string[] }) {
  return painText(row).length > 70;
}

async function showPainDetail(row: { studentName?: string; painPoints?: string[] }) {
  await ElMessageBox.alert(painText(row), `${row.studentName || "学员"}痛点`, {
    confirmButtonText: "知道了",
    customClass: "pain-detail-message"
  });
}

watch(selectedPhaseId, () => {
  loadCurrentPhaseData();
});

onMounted(async () => {
  await refreshAll();
  startCheckInSnapshotTimer();
});

onUnmounted(stopCheckInSnapshotTimer);
</script>

<template>
  <section class="page-section">
    <div class="page-heading-row course-heading">
      <div>
        <h1>课程管理</h1>
        <p>按期数组织分组、学员签到、问卷记录和课程分析。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" @click="openCreatePhase">新增期数</el-button>
      </div>
    </div>

    <div v-if="!selectedPhase" v-loading="loading" class="empty-workspace">
      <div class="empty-copy">
        <span>还没有课程期数</span>
        <h2>先创建一期课程，再录入分组和学员</h2>
        <p>创建后系统会生成专属问卷链接和二维码，学员扫码后先完成姓名签到，再进入 AI 诊断问卷。</p>
      </div>
      <el-button type="primary" size="large" @click="openCreatePhase">新增期数</el-button>
    </div>

    <template v-else>
      <div class="course-command">
        <div class="phase-picker">
          <span>当前期数</span>
          <el-select v-model="selectedPhaseId" placeholder="请选择课程期数" class="phase-select">
            <el-option v-for="phase in phases" :key="phase.id" :label="phase.phaseName" :value="phase.id" />
          </el-select>
        </div>
        <div class="phase-actions">
          <el-button @click="copySurveyUrl">复制问卷链接</el-button>
          <el-button @click="openQr">二维码</el-button>
          <el-button @click="showDashboard">数据看板</el-button>
          <el-button @click="openEditPhase()">编辑期数</el-button>
        </div>
      </div>

      <div class="phase-overview">
        <div class="phase-summary">
          <span>{{ selectedPhase.courseName || "课程" }}</span>
          <h2>{{ selectedPhase.phaseName }}</h2>
          <p>{{ selectedPhase.remark || "先创建分组并填写组长，再在分组中录入学员。学员扫码后只按姓名匹配准入。" }}</p>
        </div>
        <div class="metric-grid">
          <div class="metric-card">
            <strong>{{ dashboard?.totalStudents ?? students.length }}</strong>
            <span>学员</span>
          </div>
          <div class="metric-card">
            <strong>{{ groups.length }}</strong>
            <span>分组</span>
          </div>
          <div class="metric-card">
            <strong>{{ checkedInStudentCount }}</strong>
            <span>已签到</span>
          </div>
          <div class="metric-card">
            <strong>{{ dashboard?.submittedCount ?? records.length }}</strong>
            <span>已提交</span>
          </div>
        </div>
      </div>

      <div class="operations-grid">
        <aside class="group-sidebar">
          <div class="section-title">
            <div>
              <h2>分组</h2>
              <p>选择一个分组后维护学员名单</p>
            </div>
            <el-button size="small" type="primary" @click="openCreateGroup">新增分组</el-button>
          </div>

          <div v-loading="groupsLoading" class="group-list">
            <el-empty v-if="!groups.length" description="暂无分组" :image-size="90">
              <el-button type="primary" @click="openCreateGroup">新增分组</el-button>
            </el-empty>
            <button
              v-for="group in groups"
              :key="group.id"
              class="group-card"
              :class="{ active: selectedGroupId === group.id }"
              type="button"
              @click="selectGroup(group)"
            >
              <span class="group-card-title">{{ group.groupName }}</span>
              <span class="group-card-leader">组长 {{ group.leaderName || "未填写" }}</span>
              <span class="group-card-meta">{{ groupStudentCount(group) }} 名学员</span>
            </button>
          </div>
        </aside>

        <div class="student-panel">
          <div class="section-title">
            <div>
              <h2>{{ selectedGroup ? selectedGroup.groupName : "全部分组" }} · 学员名单</h2>
              <p>{{ groups.length ? "维护姓名、邀请人、手机号、身份证号和签到状态" : "请先创建分组，再添加学员" }}</p>
            </div>
            <el-button type="primary" :disabled="!groups.length" @click="openCreateStudent()">新增学员</el-button>
          </div>

          <el-table v-loading="studentsLoading" :data="filteredStudents" empty-text="当前分组暂无学员">
            <el-table-column prop="studentName" label="姓名" min-width="100" />
            <el-table-column prop="phone" label="手机号" min-width="130" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.isNewStudent ? 'success' : 'info'">{{ yesNo(row.isNewStudent) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="签到" width="100">
              <template #default="{ row }">
                <el-tag :type="(row.checkInCount || 0) > 0 ? 'success' : 'info'">
                  {{ (row.checkInCount || 0) > 0 ? "已签到" : "未签到" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="inviter" label="邀请人" min-width="110" show-overflow-tooltip />
            <el-table-column prop="idCard" label="身份证号" min-width="170" show-overflow-tooltip />
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column label="进入次数" width="92">
              <template #default="{ row }">{{ row.checkInCount || 0 }} 次</template>
            </el-table-column>
            <el-table-column label="操作" width="118" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEditStudent(row)">编辑</el-button>
                <el-button link type="danger" @click="removeStudent(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="records-panel">
        <div class="section-title">
          <div>
            <h2>调查问卷记录</h2>
            <p>同一学员多次填写时，列表展示最新一次提交</p>
          </div>
          <div class="record-actions">
            <el-button :disabled="!records.length" @click="removeAllRecords">全部删除</el-button>
            <el-button @click="showDashboard">数据看板</el-button>
          </div>
        </div>
        <el-table v-loading="recordsLoading" :data="records" empty-text="暂无问卷记录">
          <el-table-column prop="customerName" label="学员" min-width="110" />
          <el-table-column prop="phone" label="手机号" min-width="130" />
          <el-table-column prop="company" label="公司" min-width="180" show-overflow-tooltip />
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
    </template>
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

  <el-dialog v-model="groupDialogVisible" :title="editingGroup ? '编辑分组' : '新增分组'" width="620px" :lock-scroll="false">
    <el-form label-width="110px">
      <el-form-item label="分组名称" required>
        <el-input v-model="groupForm.groupName" placeholder="例如：第一组、A组、获客组" />
      </el-form-item>
      <el-form-item label="组长" required>
        <el-input v-model="groupForm.leaderName" placeholder="手动填写组长姓名" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="groupForm.sortOrder" :min="0" :step="1" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="groupForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="groupDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveGroup">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="studentDialogVisible" :title="editingStudent ? '编辑学员' : '新增学员'" width="620px" :lock-scroll="false">
    <el-form label-width="110px">
      <el-form-item label="所属分组" required>
        <el-select v-model="studentForm.groupId" placeholder="请选择分组">
          <el-option v-for="group in groups" :key="group.id" :label="group.groupName" :value="group.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="姓名" required>
        <el-input v-model="studentForm.studentName" placeholder="请输入学员姓名" />
      </el-form-item>
      <el-form-item label="邀请人">
        <el-input v-model="studentForm.inviter" maxlength="80" placeholder="可选填写邀请人姓名" />
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
          <template #default="{ row }">
            <div class="pain-summary-cell">
              <span class="pain-summary-text">{{ painText(row) }}</span>
              <el-button v-if="shouldShowPainDetail(row)" link type="primary" @click="showPainDetail(row)">查看全部</el-button>
            </div>
          </template>
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
.course-heading p {
  margin: 8px 0 0;
  color: #64748b;
}

.heading-actions,
.course-command,
.phase-actions,
.section-title,
.record-actions,
.analysis-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.heading-actions {
  justify-content: flex-end;
}

.empty-workspace {
  display: grid;
  justify-items: start;
  gap: 22px;
  min-height: 360px;
  padding: 52px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
}

.empty-copy {
  max-width: 600px;
}

.empty-copy span,
.phase-picker span,
.phase-summary span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.empty-copy h2,
.phase-summary h2 {
  margin: 8px 0 10px;
  color: #0f172a;
  font-size: 26px;
}

.empty-copy p,
.phase-summary p,
.section-title p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.course-command {
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
}

.phase-picker {
  display: flex;
  align-items: center;
  gap: 12px;
}

.phase-select {
  width: 360px;
}

.phase-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.phase-overview {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 520px;
  gap: 18px;
  margin-bottom: 18px;
}

.phase-summary,
.metric-card,
.group-sidebar,
.student-panel,
.records-panel {
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
}

.phase-summary {
  padding: 22px;
}

.survey-url {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  color: #475569;
  background: #f8fafc;
  word-break: break-all;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  display: grid;
  align-content: center;
  min-height: 108px;
  padding: 18px;
}

.metric-card strong {
  display: block;
  color: #2563eb;
  font-size: 32px;
  line-height: 1.1;
}

.metric-card span {
  color: #64748b;
}

.operations-grid {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
  margin-bottom: 18px;
}

.group-sidebar,
.student-panel,
.records-panel {
  padding: 14px;
}

.group-sidebar {
  position: sticky;
  top: 82px;
}

.section-title {
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-title h2 {
  margin: 0 0 4px;
  color: #0f172a;
  font-size: 18px;
}

.group-list {
  display: grid;
  gap: 8px;
  min-height: 160px;
}

.group-card {
  display: grid;
  gap: 3px;
  width: 100%;
  min-height: 74px;
  padding: 10px 12px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  color: #334155;
  text-align: left;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.group-card:hover,
.group-card.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.08);
}

.group-card-title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.group-card-leader,
.group-card-meta {
  color: #64748b;
  font-size: 12px;
}

.record-actions,
.analysis-toolbar {
  justify-content: flex-end;
}

.analysis-toolbar {
  justify-content: flex-start;
}

.analysis-toolbar .el-select {
  width: 300px;
}

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

.dashboard-content .stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
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

.pain-summary-cell {
  display: grid;
  gap: 4px;
  align-items: start;
}

.pain-summary-text {
  display: -webkit-box;
  overflow: hidden;
  color: #334155;
  line-height: 1.7;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.pain-summary-cell .el-button {
  justify-self: start;
  padding: 0;
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
  .course-command,
  .phase-picker {
    align-items: stretch;
    flex-direction: column;
  }

  .phase-overview,
  .operations-grid,
  .metric-grid,
  .dashboard-content .stat-grid {
    grid-template-columns: 1fr;
  }

  .phase-select {
    width: 100%;
  }

  .group-sidebar {
    position: static;
  }
}

@media (max-width: 720px) {
  .empty-workspace,
  .phase-summary,
  .group-sidebar,
  .student-panel,
  .records-panel {
    padding: 16px;
  }

  .section-title,
  .record-actions,
  .phase-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
