// src/components/layout/AppLayout.tsx
import React from "react";
import Navbar from "./Navbar";
import Breadcrumb from "./Breadcrumb";

/**
 * AppLayout: top navbar + breadcrumb + centered content area.
 */
const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <Breadcrumb />
      <div className="max-w-7xl mx-auto px-4 py-6">
        <main className="flex-1">{children}</main>
      </div>
    </div>
  );
};

export default AppLayout;
