// src/components/layout/Sidebar.tsx
import React from "react";
import { Link, useLocation } from "react-router-dom";
import { Home, List, Layers } from "lucide-react";

const Sidebar: React.FC<{ collapsed?: boolean }> = ({ collapsed = false }) => {
  const location = useLocation();
  const linkClass = (path: string) =>
    `flex items-center gap-3 px-3 py-2 rounded-md text-sm ${
      location.pathname === path
        ? "bg-gray-100 font-medium"
        : "text-gray-600 hover:bg-gray-50"
    }`;

  return (
    <aside
      className={`w-64 ${
        collapsed ? "hidden md:block" : "block"
      } border-r border-gray-100 bg-white`}
    >
      <div className="px-4 py-6">
        <div className="text-lg font-semibold mb-6">BrokerHub</div>
        <nav className="flex flex-col gap-1">
          <Link to="/" className={linkClass("/")}>
            <Home className="w-4 h-4" /> <span>Home</span>
          </Link>
          <Link to="/" className={linkClass("/positions")}>
            <List className="w-4 h-4" /> <span>Positions</span>
          </Link>
          <Link to="/feed" className={linkClass("/feed")}>
            <Layers className="w-4 h-4" /> <span>Feed</span>
          </Link>
        </nav>
      </div>
    </aside>
  );
};

export default Sidebar;
