<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import {
  ChatDotRound,
  ChatLineRound,
  Collection,
  Connection,
  DataAnalysis,
  DocumentChecked,
  Picture,
  User
} from "@element-plus/icons-vue";
import { useAuthStore } from "../stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const menuItems = [
  { path: "/dashboard", label: "仪表盘", icon: DataAnalysis },
  { path: "/chat", label: "AI对话", icon: ChatLineRound },
  { path: "/checkins", label: "签到列表", icon: DocumentChecked },
  { path: "/survey-records", label: "调查记录", icon: DocumentChecked },
  { path: "/users", label: "用户管理", icon: User },
  { path: "/agents", label: "智能体管理", icon: ChatDotRound },
  { path: "/scenes", label: "场景管理", icon: Collection },
  { path: "/models", label: "模型管理", icon: Connection },
  { path: "/image-storage", label: "图片存储", icon: Picture }
];
async function handleLogout() {
  await auth.signOut();
  await router.push({ name: "login" });
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="248px" class="admin-aside">
      <div class="admin-brand">
        <span>企</span>
        <div><strong>企业管理助手</strong><small>管理系统</small></div>
      </div>
      <el-menu :default-active="route.path" router class="admin-menu">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="admin-content">
      <el-header class="admin-header">
        <div>
          <strong>{{ auth.user?.displayName || auth.user?.username || "管理员" }}</strong>
          <span>{{ auth.user?.role || "admin" }}</span>
        </div>
        <el-button @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main class="admin-main" :class="{ 'admin-main-fixed': route.path === '/chat' }">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
