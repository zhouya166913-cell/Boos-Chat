<script setup lang="ts">
const stats = [
  { label: "今日新增用户", value: "0", note: "后续接入真实统计" },
  { label: "累计对话次数", value: "0", note: "后续接入真实统计" },
  { label: "已启用智能体", value: "2", note: "当前为初始化模板" },
  { label: "今日模型调用", value: "0", note: "后续接入真实统计" }
];

const tasks = [
  "先让老板实际体验智能体对话",
  "根据使用反馈继续补用户与权限能力",
  "后续接入真实统计、付费与运营数据"
];
</script>
34
<template>
  <section>
    <div class="page-heading">
      <div>
        <p>Workspace</p>
        <h1>仪表盘</h1>
      </div>
    </div>

    <div class="stat-grid">
      <el-card v-for="item in stats" :key="item.label" shadow="never" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.note }}</small>
      </el-card>
    </div>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <strong>当前阶段说明</strong>
      </template>
      <p class="dashboard-copy">
        当前仪表盘先保留为后台首页模板。真实业务重点已经转到“智能体对话”模块，
        后续等老板开始实际使用后，再把这里逐步替换成真实统计和运营数据。
      </p>
      <ul class="dashboard-list">
        <li v-for="task in tasks" :key="task">{{ task }}</li>
      </ul>
    </el-card>
  </section>
</template>
