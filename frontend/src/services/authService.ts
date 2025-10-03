// src/services/authService.ts
export interface LoginRequest {
  loginId: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  memberId: string;
  accountId: string;
  role: "ADMIN" | "MEMBER";
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
}

/**
 * Helper for POST JSON and handling error responses.
 */
async function postJson<T>(url: string, body: unknown): Promise<T> {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  let parsed: any = null;
  try {
    parsed = await res.json();
  } catch {}

  if (!res.ok) {
    const msg =
      parsed?.error ||
      parsed?.message ||
      `Request failed with status ${res.status}`;
    throw new Error(msg);
  }

  return parsed as T;
}

export async function loginService(req: LoginRequest): Promise<LoginResponse> {
  return postJson<LoginResponse>("/api/auth/login", req);
}

export const changePassword = async (payload: ChangePasswordPayload) => {
  const token = localStorage.getItem("brokerhub_token");

  if (!token) {
    throw new Error("No token found. Please log in again.");
  }

  const res = await fetch("/api/auth/change-password", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  });

  const data = await res.json();

  if (!res.ok) {
    throw new Error(data.error || "Failed to change password");
  }

  return data;
};
