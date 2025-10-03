import React from "react";
import { NavLink, Outlet, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import { User, Settings as Gear, Shield, Users } from "lucide-react";

const linkClasses = ({ isActive }: { isActive: boolean }) =>
  [
    "flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors",
    isActive
      ? "bg-blue-100 text-blue-700 font-semibold"
      : "text-gray-700 hover:bg-gray-50",
  ].join(" ");

const SettingsLayout: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const options = [
    { value: "profile", label: "Profile" },
    { value: "broker", label: "Broker Access" },
    { value: "privacy", label: "Privacy" },
    ...(user?.role === "ADMIN"
      ? [{ value: "group", label: "Group Management" }]
      : []),
  ];

  const current =
    options.find((opt) => location.pathname.includes(opt.value)) || options[0];

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto py-8 px-4 lg:px-8 flex flex-col md:flex-row gap-8">
        {/* Sidebar for desktop */}
        <aside className="hidden md:block w-64 shrink-0">
          <nav className="space-y-1">
            <NavLink to="profile" className={linkClasses}>
              {({ isActive }) => (
                <>
                  <User
                    className={`w-4 h-4 ${
                      isActive ? "text-blue-700" : "text-gray-500"
                    }`}
                  />
                  Profile
                </>
              )}
            </NavLink>

            <NavLink to="broker" className={linkClasses}>
              {({ isActive }) => (
                <>
                  <Gear
                    className={`w-4 h-4 ${
                      isActive ? "text-blue-700" : "text-gray-500"
                    }`}
                  />
                  Broker Access
                </>
              )}
            </NavLink>

            <NavLink to="privacy" className={linkClasses}>
              {({ isActive }) => (
                <>
                  <Shield
                    className={`w-4 h-4 ${
                      isActive ? "text-blue-700" : "text-gray-500"
                    }`}
                  />
                  Privacy
                </>
              )}
            </NavLink>

            {user?.role === "ADMIN" && (
              <NavLink to="group" className={linkClasses}>
                {({ isActive }) => (
                  <>
                    <Users
                      className={`w-4 h-4 ${
                        isActive ? "text-blue-700" : "text-gray-500"
                      }`}
                    />
                    Group Management
                  </>
                )}
              </NavLink>
            )}
          </nav>
        </aside>

        {/* Mobile dropdown */}
        <div className="md:hidden w-full mb-4">
          <select
            className="w-full border rounded-md p-2"
            value={current.value}
            onChange={(e) => navigate(`/settings/${e.target.value}`)}
          >
            {options.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Content */}
        <main className="flex-1 bg-white rounded-lg shadow-sm border p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default SettingsLayout;
