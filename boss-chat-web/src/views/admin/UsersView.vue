<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { listUsers, type AdminUser } from "../../api/users";

const users = ref<AdminUser[]>([]);
const loading = ref(false);
const keyword = ref("");

const filteredUsers = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  if (!term) return users.value;
  return users.value.filter((user) =>
    [user.username, user.displayName, user.mobile, user.wechatNo, user.qqNo, user.roleName]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(term))
  );
});

async function loadUsers() {
  loading.value = true;
  try {
    users.value = await listUsers();
  } finally {
    loading.value = false;
  }
}

function formatDate(value?: string) {
  return value ? value.replace("T", " ") : "-";
}

onMounted(loadUsers);
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <p>Admin</p>
        <h1>用户管理</h1>
      </div>
      <el-button @click="loadUsers">刷新</el-button>
    </div>
    <el-card shadow="never" class="panel-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索账号、姓名、手机号、微信号或 QQ 号" clearable />
      </div>
      <el-table v-loading="loading" :data="filteredUsers">
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="displayName" label="姓名" min-width="120" />
        <el-table-column prop="mobile" label="手机号" min-width="140" />
        <el-table-column prop="wechatNo" label="微信号" min-width="140" />
        <el-table-column prop="qqNo" label="QQ 号" min-width="120" />
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.roleCode === 'super_admin' ? 'danger' : 'info'">
              {{ row.roleName || "-" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? "启用" : "停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="180">
          <template #default="{ row }">{{ formatDate(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>
