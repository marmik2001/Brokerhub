import api from "../api";

export interface CreateAccountRequest {
  accountName: string;
  accountDesc?: string;
}

export interface CreateAccountResponse {
  accountId: string;
  name: string;
  role: "ADMIN" | "MEMBER";
}

/**
 * POST /api/accounts
 * Creates a new account for the authenticated user as ADMIN
 */
export async function createAccount(
  req: CreateAccountRequest
): Promise<CreateAccountResponse> {
  const { data } = await api.post<CreateAccountResponse>("/accounts", req);
  return data;
}

/**
 * GET /api/accounts
 * Lists all accounts the authenticated user belongs to
 */
export async function listAccounts(): Promise<CreateAccountResponse[]> {
  const { data } = await api.get<CreateAccountResponse[]>("/accounts");
  return data;
}
