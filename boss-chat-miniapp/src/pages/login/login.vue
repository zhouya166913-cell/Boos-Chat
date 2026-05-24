<script setup lang="ts">
import { reactive, ref } from "vue";
import { login } from "../../api/auth";

const form = reactive({
  username: "admin",
  password: ""
});
const submitting = ref(false);

async function handleLogin() {
  if (!form.username.trim() || !form.password) {
    uni.showToast({ title: "请输入账号和密码", icon: "none" });
    return;
  }

  submitting.value = true;
  try {
    const result = await login(form.username.trim(), form.password);
    uni.setStorageSync("boss-chat-miniapp-token", result.token);
    uni.showToast({ title: "登录成功", icon: "success" });
    setTimeout(() => {
      uni.reLaunch({ url: "/pages/home/home" });
    }, 300);
  } catch (error: any) {
    uni.showToast({ title: error?.message || "登录失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <view class="page">
    <view class="brand">
      <view class="logo">企</view>
      <view>
        <text class="title">企业管理助手</text>
        <text class="subtitle">小程序登录测试</text>
      </view>
    </view>

    <view class="card">
      <view class="field">
        <text class="label">账号</text>
        <input v-model="form.username" class="input" placeholder="请输入账号" />
      </view>
      <view class="field">
        <text class="label">密码</text>
        <input
          v-model="form.password"
          class="input"
          password
          placeholder="请输入密码"
          @confirm="handleLogin"
        />
      </view>
      <button class="button" :loading="submitting" @click="handleLogin">登录</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 72rpx 36rpx;
  box-sizing: border-box;
  background: linear-gradient(180deg, #eef4ff 0%, #f7f9fc 100%);
}

.brand {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 48rpx;
}

.logo {
  width: 92rpx;
  height: 92rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 42rpx;
  font-weight: 700;
  background: linear-gradient(135deg, #2f6bff 0%, #5d8cff 100%);
}

.title,
.subtitle {
  display: block;
}

.title {
  font-size: 38rpx;
  font-weight: 700;
  color: #18243a;
}

.subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6b778c;
}

.card {
  padding: 36rpx;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 18rpx 48rpx rgba(38, 61, 96, 0.08);
}

.field + .field {
  margin-top: 28rpx;
}

.label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 24rpx;
  color: #516074;
}

.input {
  height: 88rpx;
  padding: 0 24rpx;
  border-radius: 18rpx;
  background: #f4f7fb;
  color: #18243a;
}

.button {
  margin-top: 36rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 18rpx;
  color: #fff;
  background: #2f6bff;
}
</style>
