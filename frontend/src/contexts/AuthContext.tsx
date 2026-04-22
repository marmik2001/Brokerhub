import React, { createContext, useContext, useEffect, useState } from "react";
import {
  loginService,
  changePassword,
  type AuthUser,
  type AccountSummary,
} from "../services/authService";

/**
 * Interface defining the properties and methods available in the AuthContext.
 */
interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  accounts: AccountSummary[];
  currentAccount: AccountSummary | null;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => void;
  selectAccount: (accountId: string) => void;
  selectAccountDirect: (account: AccountSummary) => void;
  clearCurrentAccount: () => void;
  addAccount: (account: AccountSummary) => void;
  changePassword: (oldPassword: string, newPassword: string) => Promise<void>;
  isAuthenticated: boolean;
  isAdmin: boolean;
  /** Updates rules for an account */
  updateAccountRules: (accountId: string, rules: any) => void;
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

    const authData = {
      token: data.token,
      user: data.user,
    };

    persist(authData);

    setUser(data.user);
    setToken(data.token);
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

  const selectAccount = (accountId: string) => {
    const selected = accounts.find((a) => a.accountId === accountId) || null;
    if (!selected) return;

    const updated = {
      token,
      user,
      currentAccount: selected,
    };

    persist(updated);
    setCurrentAccount(selected);
  };

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
    };

    persist(persistent);
  };

  const addAccount = (account: AccountSummary) => {
    const newList = [...accounts, account];
    setAccounts(newList);
    setCurrentAccount(account);

    const updated = {
      token,
      user,
      currentAccount: account,
    };

    persist(updated);
  };

  const clearCurrentAccount = () => {
    setCurrentAccount(null);
    const updated = {
      token,
      user,
    };
    persist(updated);
  };

  const handleChangePassword = async (
    oldPassword: string,
    newPassword: string
  ) => {
    await changePassword({ oldPassword, newPassword });
  };

  const updateAccountRules = (accountId: string, rules: any) => {
    const updatedAccounts = accounts.map((a) =>
      a.accountId === accountId ? { ...a, rules } : a
    );
    setAccounts(updatedAccounts);

    if (currentAccount && currentAccount.accountId === accountId) {
      setCurrentAccount({ ...currentAccount, rules });
      const persistent = {
        token,
        user,
        currentAccount: { ...currentAccount, rules },
      };
      persist(persistent);
    }
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
        clearCurrentAccount,
        addAccount,
        changePassword: handleChangePassword,
        isAuthenticated: !!token,
        isAdmin,
        updateAccountRules,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
