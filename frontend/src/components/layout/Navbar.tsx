// src/components/layout/Navbar.tsx
import React, { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { User, Settings, Home, ChevronDown, LogOut, Menu } from "lucide-react";
import { useAuth } from "../../contexts/AuthContext";

const Navbar: React.FC<{ onToggleSidebar?: () => void }> = ({
  onToggleSidebar,
}) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node))
        setOpen(false);
    };
    document.addEventListener("click", onDoc);
    return () => document.removeEventListener("click", onDoc);
  }, []);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav className="bg-white border-b border-gray-200 px-4 py-3">
      <div className="flex items-center justify-between max-w-7xl mx-auto">
        <div className="flex items-center gap-4">
          <button
            onClick={onToggleSidebar}
            className="p-2 rounded-md hover:bg-gray-100 md:hidden"
            aria-label="Toggle menu"
          >
            <Menu className="w-5 h-5 text-gray-700" />
          </button>

          <Link to="/" className="text-xl font-semibold text-gray-900">
            BrokerHub
          </Link>

          <Link
            to="/"
            className="flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900 hidden md:flex"
          >
            <Home className="w-4 h-4" /> Home
          </Link>
        </div>

        <div className="relative" ref={ref}>
          <button
            onClick={() => setOpen(!open)}
            className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-gray-100"
            aria-haspopup="true"
            aria-expanded={open}
          >
            <User className="w-4 h-4 text-gray-600" />
            <span className="text-sm text-gray-700">{user?.name ?? "Me"}</span>
            <ChevronDown className="w-4 h-4 text-gray-600" />
          </button>

          {open && (
            <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-20">
              <Link
                to="/settings/profile"
                className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                onClick={() => setOpen(false)}
              >
                <User className="w-4 h-4" />
                Profile
              </Link>

              <Link
                to="/settings/broker"
                className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                onClick={() => setOpen(false)}
              >
                <Settings className="w-4 h-4" />
                Broker Access
              </Link>

              <Link
                to="/settings/privacy"
                className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                onClick={() => setOpen(false)}
              >
                <Settings className="w-4 h-4" />
                Privacy
              </Link>

              {user?.role === "ADMIN" && (
                <Link
                  to="/settings/group"
                  className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                  onClick={() => setOpen(false)}
                >
                  <Settings className="w-4 h-4" />
                  Group Management
                </Link>
              )}

              <hr className="my-1 border-gray-200" />
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
              >
                <LogOut className="w-4 h-4" />
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
