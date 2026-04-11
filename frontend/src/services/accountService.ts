import api from "../api";
import type { AccountSummary } from "./authService";

export interface CreateAccountRequest {
  accountName: string;
  accountDesc?: string;
}

export interface Member {
  memberId: string;
  loginId: string;
  email?: string;
  memberName: string;
  role: "ADMIN" | "MEMBER";
}

/**
 * POST /api/accounts
 * Creates a new account for the authenticated user as ADMIN
 */
export async function createAccount(
  req: CreateAccountRequest
): Promise<AccountSummary> {
  const { data } = await api.post<AccountSummary>("/accounts", req);
  return data;
}

/**
 * GET /api/accounts
 * Lists all accounts the authenticated user belongs to
 */
export async function listAccounts(): Promise<AccountSummary[]> {
  const { data } = await api.get<AccountSummary[]>("/accounts");
  return data;
}

/**
 * GET /api/accounts/{accountId}/members
 */
export async function getMembers(accountId: string): Promise<Member[]> {
  const { data } = await api.get<Member[]>(`/accounts/${accountId}/members`);
  return data;
}

/**
 * POST /api/accounts/{accountId}/members
 * Adds an existing user by loginId or email
 */
export async function addMember(
  accountId: string,
  payload: { loginId?: string; email?: string }
): Promise<Member> {
  const { data } = await api.post<Member>(
    `/accounts/${accountId}/members`,
    payload
  );
  return data;
}

/**
 * PATCH /api/accounts/{accountId}/members/{memberId}/role
 */
export async function updateMemberRole(
  accountId: string,
  memberId: string,
  role: "ADMIN" | "MEMBER"
): Promise<void> {
  await api.patch(`/accounts/${accountId}/members/${memberId}/role`, { role });
}

/**
 * DELETE /api/accounts/{accountId}/members/{memberId}
 */
export async function removeMember(
  accountId: string,
  memberId: string
): Promise<void> {
  await api.delete(`/accounts/${accountId}/members/${memberId}`);
}
