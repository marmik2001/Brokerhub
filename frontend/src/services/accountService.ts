// src/services/accountService.ts
export interface SignupRequest {
  accountName: string;
  accountDesc?: string;
  loginId: string;
  memberName: string;
  password: string;
}

export interface SignupResponse {
  memberId: string;
  accountId: string;
  role: "ADMIN" | "MEMBER";
}

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
    // prefer server-provided error message when available
    const msg =
      parsed?.error ||
      parsed?.message ||
      `Request failed with status ${res.status}`;
    throw new Error(msg);
  }

  return parsed as T;
}

/**
 * Call backend to create an account + initial admin member.
 * Throws Error with server message when the backend returns non-2xx.
 */
export async function signup(req: SignupRequest): Promise<SignupResponse> {
  return postJson<SignupResponse>("/api/accounts/signup", req);
}
