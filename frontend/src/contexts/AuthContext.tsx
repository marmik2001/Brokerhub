import React, { createContext, useContext, useState, useEffect } from "react";

interface AccountSummary {
  accountId: string;
  role: "ADMIN" | "MEMBER";
}

interface AuthUser {
  id: string;
  loginId: string;
  email?: string;
  name: string;
}

interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  accounts: AccountSummary[];
  activeAccount: AccountSummary | null;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => void;
  selectAccount: (accountId: string) => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [activeAccount, setActiveAccount] = useState<AccountSummary | null>(
    null
  );

  useEffect(() => {
    const stored = localStorage.getItem("brokerhub_auth");
    if (stored) {
      const parsed = JSON.parse(stored);
      setUser(parsed.user);
      setToken(parsed.token);
      setAccounts(parsed.accounts || []);
      setActiveAccount(parsed.activeAccount || null);
    }
  }, []);

  const login = async (identifier: string, password: string) => {
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identifier, password }),
    });

    if (!res.ok) throw new Error("Invalid credentials");
    const data = await res.json();

    const firstAccount = data.accounts?.[0] || null;

    const authData = {
      token: data.token,
      user: data.user,
      accounts: data.accounts,
      activeAccount: firstAccount,
    };

    localStorage.setItem("brokerhub_auth", JSON.stringify(authData));
    setUser(data.user);
    setToken(data.token);
    setAccounts(data.accounts);
    setActiveAccount(firstAccount);
  };

  const logout = () => {
    localStorage.removeItem("brokerhub_auth");
    setUser(null);
    setToken(null);
    setAccounts([]);
    setActiveAccount(null);
  };

  const selectAccount = (accountId: string) => {
    const selected = accounts.find((a) => a.accountId === accountId) || null;
    if (selected) {
      const updated = {
        token,
        user,
        accounts,
        activeAccount: selected,
      };
      localStorage.setItem("brokerhub_auth", JSON.stringify(updated));
      setActiveAccount(selected);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        accounts,
        activeAccount,
        login,
        logout,
        selectAccount,
        isAuthenticated: !!token,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
