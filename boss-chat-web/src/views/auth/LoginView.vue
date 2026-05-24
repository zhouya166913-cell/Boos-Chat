<script setup lang="ts">
/**
 * 登录页。
 *
 * 页面职责：
 * - 展示账号密码表单
 * - 将用户输入交给登录模块处理
 */
import { Lock, User } from "@element-plus/icons-vue";
import { useLoginForm } from "../../modules/auth/composables/useLoginForm";

const { form, submitting, handleSubmit } = useLoginForm();
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="brand">
        <span>企</span>
        <div>
          <h1>企业管理助手</h1>
          <p>管理系统</p>
        </div>
      </div>

      <el-form class="login-form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="请输入账号">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="handleSubmit"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>
