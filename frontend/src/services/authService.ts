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
 * AccountSummary now optionally includes accountMemberId
 * so we can store the membership UUID for per-user operations like Broker Credentials.
 */
export interface AccountSummary {
  accountId: string;
  role: "ADMIN" | "MEMBER";
  accountMemberId?: string;
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
