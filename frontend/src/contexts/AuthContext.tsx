// src/contexts/AuthContext.tsx
import React, { createContext, useContext, useState, useEffect } from "react";
import { loginService } from "../services/authService";

export interface AuthUser {
  id: string; // memberId from backend
  loginId: string;
  name: string;
  role: "ADMIN" | "MEMBER";
  token: string;
  accountId?: string;
}

interface AuthContextType {
  user: AuthUser | null;
  login: (loginId: string, password: string) => Promise<void>;
  logout: () => void;
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
  const [loading, setLoading] = useState(true);

  // On mount, hydrate from localStorage if present
  useEffect(() => {
    const token = localStorage.getItem("brokerhub_token");
    const userData = localStorage.getItem("brokerhub_user");
    if (token && userData) {
      try {
        const parsed = JSON.parse(userData) as AuthUser;
        // ensure token is in parsed object (backwards compatibility)
        if (!parsed.token) parsed.token = token;
        setUser(parsed);
      } catch (e) {
        localStorage.removeItem("brokerhub_token");
        localStorage.removeItem("brokerhub_user");
      }
    }
    setLoading(false);
  }, []);

  // Perform login via backend authService
  const login = async (loginId: string, password: string) => {
    // call backend login
    const resp = await loginService({ loginId, password });
    // resp: { token, memberId, accountId, role }
    const userObj: AuthUser = {
      id: resp.memberId,
      loginId,
      name: loginId, // backend doesn't return memberName on login; can fetch later
      role: resp.role,
      token: resp.token,
      accountId: resp.accountId,
    };

    localStorage.setItem("brokerhub_token", userObj.token);
    localStorage.setItem("brokerhub_user", JSON.stringify(userObj));
    setUser(userObj);
  };

  const logout = () => {
    localStorage.removeItem("brokerhub_token");
    localStorage.removeItem("brokerhub_user");
    setUser(null);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900" />
      </div>
    );
  }

  return (
    <AuthContext.Provider
      value={{ user, login, logout, isAuthenticated: !!user }}
    >
      {children}
    </AuthContext.Provider>
  );
};
