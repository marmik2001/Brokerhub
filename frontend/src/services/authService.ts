import api from "../api";

/**
 * Request payload for user login.
 */
export interface LoginRequest {
  identifier: string;
  password: string;
}

/**
 * Authenticated user details.
 */
export interface AuthUser {
  id: string;
  name: string;
  email: string;
  loginId: string;
}

/**
 * Account summary information including role and privacy rules.
 * Used by frontend components to render account lists and manage selection.
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
 * Response payload upon successful login.
 * Note: login no longer returns accounts; it only returns token and user details.
 */
export interface LoginResponse {
  token: string;
  user: AuthUser;
}

/**
 * Request payload to change user password.
 */
export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
}

/**
 * Authenticates a user and retrieves an access token.
 * POST /api/auth/login
 */
export async function loginService(req: LoginRequest): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>("/auth/login", req);
  return data;
}

/**
 * Updates the password for the currently authenticated user.
 * PUT /api/auth/change-password
 */
export async function changePassword(payload: ChangePasswordPayload) {
  const { data } = await api.put("/auth/change-password", payload);
  return data;
}

/**
 * Request payload for registering a new user.
 */
export interface RegisterUserRequest {
  loginId: string;
  memberName: string;
  email?: string;
  password: string;
}

/**
 * Response payload upon successful user registration.
 */
export interface RegisterUserResponse {
  id: string;
  loginId: string;
  email?: string;
  name: string;
}

/**
 * Creates a new user (without creating an account initially).
 * POST /api/user/register
 */
export async function registerUser(
  req: RegisterUserRequest
): Promise<RegisterUserResponse> {
  const { data } = await api.post<RegisterUserResponse>("/user/register", req);
  return data;
}

/**
 * Updates the privacy rules for a specific account member.
 * PATCH /api/accounts/{accountId}/members/{memberId}/rule
 * 
 * Expects the server to return the updated member payload: { memberId, accountId, rules }
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
