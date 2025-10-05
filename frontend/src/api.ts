import axios, { AxiosError } from "axios";

/**
 * Axios instance for all Brokerhub API calls.
 * Automatically attaches Authorization + X-Account-Id headers if available.
 */

const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
});

// Attach Authorization + X-Account-Id if stored
api.interceptors.request.use((config) => {
  try {
    const stored = localStorage.getItem("brokerhub_auth");
    if (stored) {
      const { token, activeAccount } = JSON.parse(stored);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      if (activeAccount?.accountId) {
        config.headers["X-Account-Id"] = activeAccount.accountId;
      }
    }
  } catch (err) {
    console.error("Error attaching auth headers", err);
  }
  return config;
});

// Centralized error handling
api.interceptors.response.use(
  (res) => res,
  (error: AxiosError<any>) => {
    const message =
      error.response?.data?.error ||
      error.response?.data?.message ||
      error.message ||
      "Network error";
    return Promise.reject(new Error(message));
  }
);

export default api;
