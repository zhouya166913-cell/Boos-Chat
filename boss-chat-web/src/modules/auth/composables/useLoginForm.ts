/**
 * 登录表单状态。
 *
 * 依赖接口：
 * - POST /api/auth/login
 *
 * 作用：
 * - 统一管理登录页表单数据和提交动作
 * - 让页面文件只保留展示和绑定职责
 */
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "../../../stores/auth";

export function useLoginForm() {
  const router = useRouter();
  const auth = useAuthStore();
  const form = reactive({ username: "admin", password: "" });
  const submitting = ref(false);

  /**
   * 提交账号密码并跳转到后台首页。
   */
  async function handleSubmit() {
    if (!form.username.trim() || !form.password) {
      return ElMessage.warning("请输入账号和密码");
    }

    submitting.value = true;
    try {
      await auth.signIn(form.username.trim(), form.password);
      ElMessage.success("登录成功");
      await router.push({ name: "dashboard" });
    } catch (error: any) {
      ElMessage.error(error?.response?.data?.message || "登录失败，请检查账号或后端服务");
    } finally {
      submitting.value = false;
    }
  }

  return {
    form,
    submitting,
    handleSubmit
  };
}
