<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { ref } from "vue";
import { logout, me } from "../../api/auth";
import type { CurrentUser } from "../../types/auth";

const user = ref<CurrentUser | null>(null);
const loading = ref(false);

async function loadCurrentUser() {
  loading.value = true;
  try {
    user.value = await me();
  } catch {
    uni.removeStorageSync("boss-chat-miniapp-token");
    uni.reLaunch({ url: "/pages/login/login" });
  } finally {
    loading.value = false;
  }
}

async function handleLogout() {
  try {
    await logout();
  } finally {
    uni.removeStorageSync("boss-chat-miniapp-token");
    uni.reLaunch({ url: "/pages/login/login" });
  }
}

onShow(() => {
  loadCurrentUser();
});
</script>

<template>
  <view class="page">
    <view class="header">
      <text class="eyebrow">已登录</text>
      <text class="title">欢迎回来，{{ user?.displayName || "管理员" }}</text>
      <text class="subtitle">这里是小程序端的登录联调占位首页</text>
    </view>

    <view class="card">
      <view class="row">
        <text class="label">账号</text>
        <text>{{ user?.username || "-" }}</text>
      </view>
      <view class="row">
        <text class="label">角色</text>
        <text>{{ user?.role || "-" }}</text>
      </view>
      <view class="row">
        <text class="label">状态</text>
        <text>{{ loading ? "同步中" : "接口正常" }}</text>
      </view>
    </view>

    <button class="button" @click="handleLogout">退出登录</button>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 72rpx 36rpx;
  box-sizing: border-box;
  background: #f7f9fc;
}

.header {
  margin-bottom: 36rpx;
}

.eyebrow,
.title,
.subtitle {
  display: block;
}

.eyebrow {
  color: #2f6bff;
  font-size: 24rpx;
}

.title {
  margin-top: 12rpx;
  color: #18243a;
  font-size: 38rpx;
  font-weight: 700;
}

.subtitle {
  margin-top: 10rpx;
  color: #6b778c;
  font-size: 24rpx;
}

.card {
  padding: 32rpx;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 18rpx 48rpx rgba(38, 61, 96, 0.08);
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  color: #18243a;
}

.row + .row {
  margin-top: 24rpx;
}

.label {
  color: #6b778c;
}

.button {
  margin-top: 36rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 18rpx;
  color: #fff;
  background: #18243a;
}
</style>