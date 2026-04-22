import api from "../api";
import type { AccountSummary } from "./authService";

/**
 * Request payload for creating a new account.
 */
export interface CreateAccountRequest {
  accountName: string;
  accountDesc?: string;
}

/**
 * Details of an account member.
 */
export interface Member {
  memberId: string;
  loginId: string;
  email?: string;
  memberName: string;
  role: "ADMIN" | "MEMBER";
}

/**
 * Creates a new account for the authenticated user as ADMIN.
 * POST /api/accounts
 */
export async function createAccount(
  req: CreateAccountRequest
): Promise<AccountSummary> {
  const { data } = await api.post<AccountSummary>("/accounts", req);
  return data;
}

/**
 * Lists all accounts the authenticated user belongs to.
 * GET /api/accounts
 */
export async function listAccounts(): Promise<AccountSummary[]> {
  const { data } = await api.get<AccountSummary[]>("/accounts");
  return data;
}

/**
 * Fetches all members for a specific account.
 * GET /api/accounts/{accountId}/members
 */
export async function getMembers(accountId: string): Promise<Member[]> {
  const { data } = await api.get<Member[]>(`/accounts/${accountId}/members`);
  return data;
}

/**
 * Adds an existing user by loginId or email to an account.
 * POST /api/accounts/{accountId}/members
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
 * Updates the role of a specific member in an account.
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
 * Removes a member from an account.
 * DELETE /api/accounts/{accountId}/members/{memberId}
 */
export async function removeMember(
  accountId: string,
  memberId: string
): Promise<void> {
  await api.delete(`/accounts/${accountId}/members/${memberId}`);
}
