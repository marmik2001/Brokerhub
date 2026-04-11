// src/components/layout/Navbar.tsx
import React, { useState, useRef, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  User,
  ChevronDown,
  LogOut,
  Settings as Gear,
  Layers,
} from "lucide-react";
import { useAuth } from "../../contexts/AuthContext";

const Navbar: React.FC<{
  showPrimaryNav?: boolean;
  showChangeGroup?: boolean;
}> = ({
  showPrimaryNav = true,
  showChangeGroup = true,
}) => {
  const { user, logout, currentAccount, clearCurrentAccount } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
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

  const goToSettings = () => {
    setOpen(false);
    navigate("/settings/profile");
  };

  const goToGroupSelect = () => {
    setOpen(false);
    clearCurrentAccount();
    navigate("/select-account");
  };

  type Section = "holding" | "positions" | "feed";
  const selectedSection =
    (location.state as { section?: Section } | null)?.section ?? "holding";

  const navLinkClass = (isActive: boolean) =>
    [
      "text-sm px-3 py-1.5 rounded-md transition-colors",
      isActive
        ? "bg-gray-100 text-gray-900 font-medium"
        : "text-gray-600 hover:text-gray-900 hover:bg-gray-50",
    ].join(" ");

  const goToHomeSection = (section: Section) => {
    navigate("/", { state: { section } });
  };
  const canShowPrimaryNav = showPrimaryNav && !!currentAccount;

  return (
    <nav className="bg-white border-b border-gray-200 px-4 py-3">
      <div className="flex items-center justify-between max-w-7xl mx-auto">
        <div className="flex items-center gap-4">
          <span className="text-xl font-semibold text-gray-900">
            BrokerHub
          </span>

          {canShowPrimaryNav && (
            <>
              <div className="hidden md:flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => goToHomeSection("holding")}
                  className={navLinkClass(
                    location.pathname === "/" && selectedSection === "holding"
                  )}
                >
                  Holding
                </button>
                <button
                  type="button"
                  onClick={() => goToHomeSection("positions")}
                  className={navLinkClass(
                    location.pathname === "/" && selectedSection === "positions"
                  )}
                >
                  Positions
                </button>
                <button
                  type="button"
                  onClick={() => goToHomeSection("feed")}
                  className={navLinkClass(
                    location.pathname === "/" && selectedSection === "feed"
                  )}
                >
                  Feed
                </button>
              </div>
            </>
          )}
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
              <button
                onClick={goToSettings}
                className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 w-full text-left"
              >
                <Gear className="w-4 h-4" /> Settings
              </button>
              {showChangeGroup && (
                <button
                  onClick={goToGroupSelect}
                  className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 w-full text-left"
                >
                  <Layers className="w-4 h-4" /> Change Group
                </button>
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
