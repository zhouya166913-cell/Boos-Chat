import { defineStore } from "pinia";
import { login, logout, me, type CurrentUser } from "../api/auth";

const TOKEN_KEY = "boss-chat-token";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || "",
    user: null as CurrentUser | null,
    loading: false
  }),
  actions: {
    async signIn(username: string, password: string) {
      this.loading = true;
      try {
        const result = await login({ username, password });
        this.token = result.token;
        this.user = result.user;
        localStorage.setItem(TOKEN_KEY, result.token);
      } finally {
        this.loading = false;
      }
    },
    async loadCurrentUser() {
      if (!this.token) return;
      this.user = await me();
    },
    async signOut() {
      try {
        await logout();
      } finally {
        this.token = "";
        this.user = null;
        localStorage.removeItem(TOKEN_KEY);
      }
    }
  }
});
