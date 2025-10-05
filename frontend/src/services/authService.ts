import api from "../api";

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email?: string;
  loginId: string;
}

export interface AccountSummary {
  accountId: string;
  role: "ADMIN" | "MEMBER";
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
  accounts: AccountSummary[];
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
}

/**
 * POST /api/auth/login
 */
export async function loginService(req: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>("/auth/login", req);
  return data;
}

/**
 * PUT /api/auth/change-password
 */
export async function changePassword(payload: ChangePasswordPayload) {
  const { data } = await api.put("/auth/change-password", payload);
  return data;
}
