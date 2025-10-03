import React, { createContext, useContext, useState, useEffect } from "react";
import { loginService, type AuthUser } from "../services/authService";

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

  useEffect(() => {
    const token = localStorage.getItem("brokerhub_token");
    const userData = localStorage.getItem("brokerhub_user");
    if (token && userData) {
      try {
        setUser(JSON.parse(userData) as AuthUser);
      } catch {
        localStorage.removeItem("brokerhub_token");
        localStorage.removeItem("brokerhub_user");
      }
    }
    setLoading(false);
  }, []);

  const login = async (loginId: string, password: string) => {
    const u = await loginService(loginId, password);
    localStorage.setItem("brokerhub_token", u.token);
    localStorage.setItem("brokerhub_user", JSON.stringify(u));
    setUser(u);
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
