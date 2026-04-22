import api from "../api";

/**
 * Details of a stored broker credential.
 */
export interface BrokerCredential {
  credentialId: string;
  accountMemberId?: string;
  nickname: string;
  broker: string;
  createdAt?: string;
}

/**
 * Fetches a list of broker credentials for a specific membership.
 * GET /api/brokers
 *
 * Preferred: pass accountMemberId (the account_member.id for the current user's membership).
 * Fallback: if accountMemberId is not provided, the function will try to use accountId query param
 * (i.e., GET /api/brokers?accountId=<id>) — backend may support only accountMemberId; prefer providing accountMemberId.
 */
export async function listCredentials(
  accountMemberId?: string,
  accountId?: string
): Promise<BrokerCredential[]> {
  const params: Record<string, string> = {};
  if (accountMemberId) params.accountMemberId = accountMemberId;
  else if (accountId) params.accountId = accountId;

  const { data } = await api.get<BrokerCredential[]>("/brokers", { params });
  return data;
}

/**
 * Stores a new broker credential.
 * POST /api/brokers
 * 
 * Payload requirements:
 * - accountMemberId is required (the membership id).
 * - token should be a string (token JSON or raw token).
 */
export async function storeCredential(payload: {
  accountMemberId: string;
  broker: string;
  nickname: string;
  token: string;
}): Promise<BrokerCredential> {
  const { data } = await api.post<BrokerCredential>("/brokers", payload);
  return data;
}

/**
 * Deletes a stored credential by its ID.
 * DELETE /api/brokers/{credentialId}
 */
export async function deleteCredential(credentialId: string): Promise<void> {
  await api.delete(`/brokers/${credentialId}`);
}
