// src/App.tsx
import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "react-hot-toast";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import SettingsProfilePage from "./pages/settings/ProfilePage";
import SettingsBrokerPage from "./pages/settings/BrokerPage";
import SettingsPrivacyPage from "./pages/settings/PrivacyPage";
import SettingsGroupPage from "./pages/settings/GroupPage";
import AppLayout from "./components/layout/AppLayout";
import SignupPage from "./pages/SignupPage.tsx";

export default function App() {
  return (
    <BrowserRouter>
      {/* Global toaster for toast messages */}
      <Toaster position="top-right" />

      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          {/* Public signup placeholder route (SignupPage will replace this later) */}
          <Route path="/signup" element={<SignupPage />} />

          {/* All protected routes */}
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route
                      path="/settings/profile"
                      element={<SettingsProfilePage />}
                    />
                    <Route
                      path="/settings/broker"
                      element={<SettingsBrokerPage />}
                    />
                    <Route
                      path="/settings/privacy"
                      element={<SettingsPrivacyPage />}
                    />
                    <Route
                      path="/settings/group"
                      element={<SettingsGroupPage />}
                    />
                    <Route path="*" element={<Navigate to="/" replace />} />
                  </Routes>
                </AppLayout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
