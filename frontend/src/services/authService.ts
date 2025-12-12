import api from "../api";

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  loginId: string;
}

/**
 * AccountSummary includes name/description and accountMemberId
 * so frontend components (SelectAccount) can render full info returned by GET /api/accounts.
 */
export interface AccountSummary {
  accountId: string;
  name?: string;
  description?: string;
  role: "ADMIN" | "MEMBER";
  accountMemberId: string;
  rules?: { privacy?: "DETAILED" | "SUMMARY" | "PRIVATE" } | any;
}

/**
 * LoginResponse: NOTE - login no longer returns accounts. We only get token + user.
 */
export interface LoginResponse {
  token: string;
  user: AuthUser;
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

/**
 * POST /api/user/register
 * Creates a new user (without creating an account)
 */
export interface RegisterUserRequest {
  loginId: string;
  memberName: string;
  email?: string;
  password: string;
}

export interface RegisterUserResponse {
  id: string;
  loginId: string;
  email?: string;
  name: string;
}

export async function registerUser(
  req: RegisterUserRequest
): Promise<RegisterUserResponse> {
  const { data } = await api.post<RegisterUserResponse>("/user/register", req);
  return data;
}

/**
 * PATCH /api/accounts/{accountId}/members/{memberId}/rule
 * Minimal wrapper to update member privacy.
 * Expects server to return updated member payload { memberId, accountId, rules }
 */
export async function updateMemberRule(
  accountId: string,
  accountMemberId: string,
  privacy: "DETAILED" | "SUMMARY" | "PRIVATE"
) {
  const { data } = await api.patch(
    `/accounts/${accountId}/members/${accountMemberId}/rule`,
    { privacy }
  );
  return data;
}
