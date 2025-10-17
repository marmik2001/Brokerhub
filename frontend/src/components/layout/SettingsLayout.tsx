import React from "react";
import { NavLink, Outlet, useNavigate, useLocation } from "react-router-dom";
import { User, Settings as Gear, Shield, Users } from "lucide-react";
import { useAuth } from "../../contexts/AuthContext";

const linkClasses = ({ isActive }: { isActive: boolean }) =>
  [
    "flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors",
    isActive
      ? "bg-blue-100 text-blue-700 font-semibold"
      : "text-gray-700 hover:bg-gray-50",
  ].join(" ");

const SettingsLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAdmin } = useAuth();

  // Base options; "group" will be conditionally included below
  const baseOptions = [
    { value: "profile", label: "Profile" },
    { value: "broker", label: "Broker Access" },
    { value: "privacy", label: "Privacy" },
    { value: "group", label: "Group Management" },
  ];

  // Filter options for non-admin users (hide Group Management)
  const options = isAdmin
    ? baseOptions
    : baseOptions.filter((o) => o.value !== "group");

  // If route contains a segment that matches an available option, pick it; otherwise fall back to first
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

            {/* Conditionally render Group Management for admins only */}
            {isAdmin && (
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
