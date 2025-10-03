export interface AuthUser {
  id: string;
  loginId: string;
  name: string;
  role: "ADMIN" | "MEMBER";
  token: string;
}

// simple mock login service
export async function loginService(
  loginId: string,
  password: string
): Promise<AuthUser> {
  // simulate network latency
  await new Promise((r) => setTimeout(r, 200));

  if (loginId === "testuser" && password === "password123") {
    return {
      id: "1",
      loginId,
      name: "Test User",
      role: "ADMIN",
      token: "mock-jwt-token-12345",
    };
  }

  // else: failure
  throw new Error("Invalid credentials");
}
