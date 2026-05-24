import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 9000,
    proxy: {
      "/api": {
        target: "http://localhost:9090",
        changeOrigin: true
      }
    }
  }
});
