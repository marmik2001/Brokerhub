// src/components/layout/AppLayout.tsx
import React, { useState } from "react";
import Navbar from "./Navbar";
import Breadcrumb from "./Breadcrumb";
import Sidebar from "./Sidebar";

const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar onToggleSidebar={() => setSidebarOpen((s) => !s)} />
      <Breadcrumb />
      <div className="max-w-7xl mx-auto px-4 py-6 flex gap-6">
        {/* Sidebar - collapses on small screens */}
        <div className={`hidden md:block w-64`}>
          <Sidebar collapsed={!sidebarOpen} />
        </div>

        {/* Main content */}
        <main className="flex-1">{children}</main>
      </div>
    </div>
  );
};

export default AppLayout;
