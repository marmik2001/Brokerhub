// src/App.tsx
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import SettingsProfilePage from "./pages/settings/ProfilePage";
import SettingsBrokerPage from "./pages/settings/BrokerPage";
import SettingsPrivacyPage from "./pages/settings/PrivacyPage";
import SettingsGroupPage from "./pages/settings/GroupPage";
import AppLayout from "./components/layout/AppLayout";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          {/* All protected routes go under this single block */}
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
