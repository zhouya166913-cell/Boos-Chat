<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import QRCode from "qrcode";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createCoursePhase,
  createCourseStudent,
  deleteCourseStudent,
  getCourseDashboard,
  listCoursePhases,
  listCourseStudents,
  updateCoursePhase,
  updateCourseStudent,
  type CourseDashboard,
  type CoursePhase,
  type CourseStudent
} from "../../api/coursePhases";

const phases = ref<CoursePhase[]>([]);
const loading = ref(false);
const phaseDialogVisible = ref(false);
const studentDrawerVisible = ref(false);
const studentDialogVisible = ref(false);
const qrDialogVisible = ref(false);
const dashboardDrawerVisible = ref(false);
const editingPhase = ref<CoursePhase | null>(null);
const selectedPhase = ref<CoursePhase | null>(null);
const editingStudent = ref<CourseStudent | null>(null);
const students = ref<CourseStudent[]>([]);
const dashboard = ref<CourseDashboard | null>(null);
const generatedQrImage = ref("");

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

const qrImage = computed(() => selectedPhase.value?.qrImageUrl || generatedQrImage.value);

function phaseSurveyUrl(phase: CoursePhase) {
  const baseUrl = import.meta.env.VITE_SURVEY_PUBLIC_URL || `${window.location.origin}${phase.surveyPath || "/survey/enterprise-diagnosis.html"}`;
  const url = new URL(baseUrl, window.location.origin);
  url.searchParams.set("phase", phase.phaseCode);
  return url.toString();
}

async function loadPhases() {
  loading.value = true;
  try {
    phases.value = await listCoursePhases();
  } finally {
    loading.value = false;
  }
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

function openEditPhase(phase: CoursePhase) {
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
  if (editingPhase.value) {
    await updateCoursePhase(editingPhase.value.id, payload);
    ElMessage.success("期数已更新");
  } else {
    await createCoursePhase(payload);
    ElMessage.success("期数已创建");
  }
  phaseDialogVisible.value = false;
  await loadPhases();
}

async function openStudents(phase: CoursePhase) {
  selectedPhase.value = phase;
  students.value = await listCourseStudents(phase.id);
  studentDrawerVisible.value = true;
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
  resetStudentForm();
  studentDialogVisible.value = true;
}

function openEditStudent(student: CourseStudent) {
  editingStudent.value = student;
  studentForm.studentName = student.studentName;
  studentForm.phone = student.phone;
  studentForm.idCard = student.idCard || "";
  studentForm.isNewStudent = student.isNewStudent;
  studentForm.remark = student.remark || "";
  studentDialogVisible.value = true;
}

async function saveStudent() {
  if (!selectedPhase.value) return;
  if (!studentForm.studentName.trim() || !studentForm.phone.trim()) {
    ElMessage.warning("请填写学员姓名和手机号");
    return;
  }
  const payload = {
    studentName: studentForm.studentName.trim(),
    phone: studentForm.phone.trim(),
    idCard: studentForm.idCard.trim(),
    isNewStudent: studentForm.isNewStudent,
    remark: studentForm.remark.trim()
  };
  if (editingStudent.value) {
    await updateCourseStudent(selectedPhase.value.id, editingStudent.value.id, payload);
    ElMessage.success("学员已更新");
  } else {
    await createCourseStudent(selectedPhase.value.id, payload);
    ElMessage.success("学员已新增");
  }
  studentDialogVisible.value = false;
  students.value = await listCourseStudents(selectedPhase.value.id);
  await loadPhases();
}

async function removeStudent(student: CourseStudent) {
  if (!selectedPhase.value) return;
  await ElMessageBox.confirm(`确定删除学员「${student.studentName}」吗？`, "删除学员", { type: "warning" });
  await deleteCourseStudent(selectedPhase.value.id, student.id);
  students.value = await listCourseStudents(selectedPhase.value.id);
  await loadPhases();
  ElMessage.success("学员已删除");
}

async function openQr(phase: CoursePhase) {
  selectedPhase.value = phase;
  generatedQrImage.value = await QRCode.toDataURL(phaseSurveyUrl(phase), {
    width: 320,
    margin: 2,
    color: { dark: "#111827", light: "#ffffff" }
  });
  qrDialogVisible.value = true;
}

async function copySurveyUrl(phase: CoursePhase) {
  await navigator.clipboard.writeText(phaseSurveyUrl(phase));
  ElMessage.success("问卷链接已复制");
}

function downloadQrImage() {
  if (!selectedPhase.value || !qrImage.value) return;
  const link = document.createElement("a");
  link.href = qrImage.value;
  link.download = `${selectedPhase.value.phaseName}-问卷二维码.png`;
  link.click();
}

async function openDashboard(phase: CoursePhase) {
  selectedPhase.value = phase;
  dashboard.value = await getCourseDashboard(phase.id);
  dashboardDrawerVisible.value = true;
}

onMounted(loadPhases);
</script>

<template>
  <section class="page-section">
    <div class="page-heading-row">
      <div>
        <p class="eyebrow">Course</p>
        <h1>签到列表</h1>
      </div>
      <el-button type="primary" @click="openCreatePhase">新增期数</el-button>
    </div>

    <div class="table-card">
      <el-alert
        title="每一期会生成独立问卷链接和二维码。学员提交问卷后，记录会自动归档到对应期数；手机号匹配时会自动关联学员。"
        type="info"
        :closable="false"
        show-icon
      />
      <el-table v-loading="loading" :data="phases" class="mt-table">
        <el-table-column prop="phaseName" label="期数" min-width="190" />
        <el-table-column prop="courseName" label="课程" min-width="150" />
        <el-table-column label="学员" width="90">
          <template #default="{ row }">{{ row.studentCount }}</template>
        </el-table-column>
        <el-table-column label="问卷" width="90">
          <template #default="{ row }">{{ row.surveyRecordCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? "启用" : "停用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copySurveyUrl(row)">复制链接</el-button>
            <el-button link type="primary" @click="openQr(row)">二维码</el-button>
            <el-button link type="primary" @click="openStudents(row)">学员</el-button>
            <el-button link type="primary" @click="openDashboard(row)">数据看板</el-button>
            <el-button link @click="openEditPhase(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>

  <el-dialog v-model="phaseDialogVisible" :title="editingPhase ? '编辑期数' : '新增期数'" width="620px" :lock-scroll="false">
    <el-form label-width="120px">
      <el-form-item label="期数名称" required>
        <el-input v-model="phaseForm.phaseName" placeholder="例如：第十三期AI运营操盘手" />
      </el-form-item>
      <el-form-item label="课程名称">
        <el-input v-model="phaseForm.courseName" placeholder="AI运营操盘手" />
      </el-form-item>
      <el-form-item label="二维码图片URL">
        <el-input v-model="phaseForm.qrImageUrl" placeholder="可选：填入图床二维码图片地址后会替换系统生成二维码" />
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

  <el-dialog v-model="qrDialogVisible" title="期数问卷二维码" width="500px" :lock-scroll="false">
    <div v-if="selectedPhase" class="qr-box">
      <img :src="qrImage" alt="问卷二维码" />
      <strong>{{ selectedPhase.phaseName }}</strong>
      <p>{{ phaseSurveyUrl(selectedPhase) }}</p>
      <el-button type="primary" @click="downloadQrImage">保存图片</el-button>
    </div>
  </el-dialog>

  <el-drawer v-model="studentDrawerVisible" :title="selectedPhase?.phaseName || '学员列表'" size="720px" :lock-scroll="false">
    <div class="drawer-toolbar">
      <el-button type="primary" @click="openCreateStudent">新增学员</el-button>
    </div>
    <el-table :data="students">
      <el-table-column prop="studentName" label="姓名" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="idCard" label="身份证号" min-width="180" />
      <el-table-column label="新学员" width="90">
        <template #default="{ row }">
          <el-tag :type="row.isNewStudent ? 'success' : 'info'">{{ row.isNewStudent ? "是" : "否" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditStudent(row)">编辑</el-button>
          <el-button link type="danger" @click="removeStudent(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>

  <el-dialog v-model="studentDialogVisible" :title="editingStudent ? '编辑学员' : '新增学员'" width="560px" :lock-scroll="false">
    <el-form label-width="100px">
      <el-form-item label="姓名" required>
        <el-input v-model="studentForm.studentName" />
      </el-form-item>
      <el-form-item label="手机号" required>
        <el-input v-model="studentForm.phone" maxlength="11" />
      </el-form-item>
      <el-form-item label="身份证号">
        <el-input v-model="studentForm.idCard" />
      </el-form-item>
      <el-form-item label="新学员">
        <el-switch v-model="studentForm.isNewStudent" :active-value="1" :inactive-value="0" />
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
</template>

<style scoped>
.page-heading-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 800;
}

.page-heading-row h1 {
  margin: 0;
  font-size: 30px;
}

.table-card {
  padding: 24px;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.mt-table {
  margin-top: 18px;
}

.qr-box {
  display: grid;
  justify-items: center;
  gap: 14px;
  text-align: center;
}

.qr-box img {
  width: 280px;
  height: 280px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.qr-box p {
  width: 100%;
  margin: 0;
  padding: 10px 12px;
  color: #64748b;
  background: #f8fafc;
  border-radius: 8px;
  word-break: break-all;
}

.drawer-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
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
</style>
