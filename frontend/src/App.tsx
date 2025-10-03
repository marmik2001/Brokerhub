// src/App.tsx
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
import SettingsLayout from "./components/layout/SettingsLayout";
import SignupPage from "./pages/SignupPage";

export default function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />

      <AuthProvider>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          {/* Protected app routes */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <HomePage />
                </AppLayout>
              </ProtectedRoute>
            }
          />

          {/* Settings section with sidebar */}
          <Route
            path="/settings"
            element={
              <ProtectedRoute>
                <AppLayout>
                  <SettingsLayout />
                </AppLayout>
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="profile" replace />} />
            <Route path="profile" element={<SettingsProfilePage />} />
            <Route path="broker" element={<SettingsBrokerPage />} />
            <Route path="privacy" element={<SettingsPrivacyPage />} />
            <Route path="group" element={<SettingsGroupPage />} />
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
