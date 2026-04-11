import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const apiProxyTarget =
    mode === "development" ? "http://localhost:8080" : "http://backend:8080";

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": {
          target: apiProxyTarget,
          changeOrigin: true,
          secure: false,
        },
      },
    },
  };
});
