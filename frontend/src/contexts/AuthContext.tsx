import React, { createContext, useContext, useEffect, useState } from "react";
import api from "../api";
import {
  loginService,
  changePassword,
  type AuthUser,
  type AccountSummary,
} from "../services/authService";

interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  accounts: AccountSummary[];
  currentAccount: AccountSummary | null;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => void;
  selectAccount: (accountId: string) => void;
  addAccount: (account: AccountSummary) => void;
  changePassword: (oldPassword: string, newPassword: string) => Promise<void>;
  isAuthenticated: boolean;
  isAdmin: boolean;
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
  const [currentAccount, setCurrentAccount] = useState<AccountSummary | null>(
    null
  );

  useEffect(() => {
    const stored = localStorage.getItem("brokerhub_auth");
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setUser(parsed.user || null);
        setToken(parsed.token || null);
        setAccounts(parsed.accounts || []);
        setCurrentAccount(parsed.currentAccount || null);
      } catch (err) {
        console.error("Failed to parse stored auth", err);
      }
    }
  }, []);

  const persist = (data: any) => {
    localStorage.setItem("brokerhub_auth", JSON.stringify(data));
  };

  const login = async (identifier: string, password: string) => {
    const data = await loginService({ identifier, password });
    const firstAccount = data.accounts?.[0] || null;
    const authData = {
      token: data.token,
      user: data.user,
      accounts: data.accounts,
      currentAccount: firstAccount,
    };
    persist(authData);
    setUser(data.user);
    setToken(data.token);
    setAccounts(data.accounts);
    setCurrentAccount(firstAccount);
  };

  const logout = () => {
    localStorage.removeItem("brokerhub_auth");
    setUser(null);
    setToken(null);
    setAccounts([]);
    setCurrentAccount(null);
  };

  /**
   * Select an account by accountId.
   *
   * Behavior:
   *  - Immediately persist the selection (without accountMemberId).
   *  - Fire a background request GET /api/accounts/{accountId}/membership to fetch the
   *    current user's membership id (accountMemberId). On success, update persisted state
   *    and `currentAccount` to include accountMemberId so downstream services can use it.
   *
   * This keeps the select-account call synchronous (no API-await required by callers),
   * while ensuring membership-id is resolved shortly after selection.
   */
  const selectAccount = (accountId: string) => {
    const selected = accounts.find((a) => a.accountId === accountId) || null;
    if (!selected) return;

    const updated = { token, user, accounts, currentAccount: selected };
    persist(updated);
    setCurrentAccount(selected);

    // Background fetch membership for the selected account and update persisted state.
    (async () => {
      try {
        const resp = await api.get(`/accounts/${accountId}/membership`);
        const data = resp.data as {
          accountMemberId: string;
          accountId: string;
          role: string;
        };

        // merge accountMemberId into selected and into accounts list
        const withMember: AccountSummary = {
          ...selected,
          accountMemberId: data.accountMemberId,
          role: data.role as "ADMIN" | "MEMBER",
        };

        // update accounts array: replace the matching account with enriched object
        const newAccounts = accounts.map((a) =>
          a.accountId === accountId ? withMember : a
        );

        const persistent = {
          token,
          user,
          accounts: newAccounts,
          currentAccount: withMember,
        };

        persist(persistent);
        setAccounts(newAccounts);
        setCurrentAccount(withMember);
      } catch (err) {
        // fail silently but log — selection remains usable, but accountMemberId won't be present
        console.error("Failed to fetch membership for account", accountId, err);
      }
    })();
  };

  const addAccount = (account: AccountSummary) => {
    const updated = {
      token,
      user,
      accounts: [...accounts, account],
      currentAccount: account,
    };
    persist(updated);
    setAccounts((prev) => [...prev, account]);
    setCurrentAccount(account);
  };

  const handleChangePassword = async (
    oldPassword: string,
    newPassword: string
  ) => {
    await changePassword({ oldPassword, newPassword });
  };

  const isAdmin = currentAccount?.role === "ADMIN";

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        accounts,
        currentAccount,
        login,
        logout,
        selectAccount,
        addAccount,
        changePassword: handleChangePassword,
        isAuthenticated: !!token,
        isAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
