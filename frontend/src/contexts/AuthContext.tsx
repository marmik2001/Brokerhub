import React, { createContext, useContext, useEffect, useState } from "react";
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
  selectAccountDirect: (account: AccountSummary) => void;
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

  // accounts + currentAccount still exist as state — but login no longer fills them
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

        // NOTE: we NO LONGER load accounts/currentAccount from storage
        // Accounts must be fetched from GET /accounts
      } catch (err) {
        console.error("Failed to parse stored auth", err);
      }
    }
  }, []);

  const persist = (data: any) => {
    localStorage.setItem("brokerhub_auth", JSON.stringify(data));
  };

  /**
   * Login no longer stores accounts or currentAccount.
   */
  const login = async (identifier: string, password: string) => {
    const data = await loginService({ identifier, password });

    const authData = {
      token: data.token,
      user: data.user,
      // accounts removed
      // currentAccount removed
    };

    persist(authData);

    setUser(data.user);
    setToken(data.token);

    // clear accounts and currentAccount from state on login
    setAccounts([]);
    setCurrentAccount(null);
  };

  const logout = () => {
    localStorage.removeItem("brokerhub_auth");
    setUser(null);
    setToken(null);
    setAccounts([]);
    setCurrentAccount(null);
  };

  /**
   * selectAccount persists only token + user + currentAccount.
   */
  const selectAccount = (accountId: string) => {
    const selected = accounts.find((a) => a.accountId === accountId) || null;
    if (!selected) return;

    const updated = {
      token,
      user,
      currentAccount: selected,
      // accounts are NOT stored anymore
    };

    persist(updated);
    setCurrentAccount(selected);
  };

  /**
   * selectAccountDirect persists only token + user + currentAccount.
   */
  const selectAccountDirect = (account: AccountSummary) => {
    const newAccounts = accounts.some((a) => a.accountId === account.accountId)
      ? accounts.map((a) => (a.accountId === account.accountId ? account : a))
      : [...accounts, account];

    setAccounts(newAccounts);
    setCurrentAccount(account);

    const persistent = {
      token,
      user,
      currentAccount: account,
      // accounts not persisted
    };

    persist(persistent);
  };

  const addAccount = (account: AccountSummary) => {
    const newList = [...accounts, account];
    setAccounts(newList);
    setCurrentAccount(account);

    // persist only token + user + currentAccount (NOT accounts)
    const updated = {
      token,
      user,
      currentAccount: account,
    };

    persist(updated);
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
        selectAccountDirect,
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
