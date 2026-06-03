import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/auth/LoginView.vue";
import AdminLayout from "../layouts/AdminLayout.vue";
import DashboardView from "../views/admin/DashboardView.vue";
import ChatView from "../views/admin/ChatView.vue";
import CheckinListView from "../views/admin/CheckinListView.vue";
import SurveyRecordsView from "../views/admin/SurveyRecordsView.vue";
import UsersView from "../views/admin/UsersView.vue";
import AgentsView from "../views/admin/AgentsView.vue";
import ScenesView from "../views/admin/ScenesView.vue";
import ModelManagementView from "../views/admin/ModelManagementView.vue";
import ImageStorageView from "../views/admin/ImageStorageView.vue";
import { useAuthStore } from "../stores/auth";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/dashboard" },
    { path: "/login", name: "login", component: LoginView },
    {
      path: "/",
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "dashboard", name: "dashboard", component: DashboardView },
        { path: "chat", name: "chat", component: ChatView },
        { path: "checkins", name: "checkins", component: CheckinListView },
        { path: "survey-records", name: "survey-records", component: SurveyRecordsView },
        { path: "users", name: "users", component: UsersView },
        { path: "agents", name: "agents", component: AgentsView },
        { path: "scenes", name: "scenes", component: ScenesView },
        { path: "models", name: "models", component: ModelManagementView },
        { path: "image-storage", name: "image-storage", component: ImageStorageView }
      ]
    }
  ]
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.token) return { name: "login" };
  if (to.name === "login" && auth.token) return { name: "dashboard" };
  return true;
});
