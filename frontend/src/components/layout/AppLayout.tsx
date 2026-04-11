// src/components/layout/AppLayout.tsx
import React from "react";
import Navbar from "./Navbar";

/**
 * AppLayout: top navbar + centered content area.
 */
const AppLayout: React.FC<{
  children: React.ReactNode;
  showPrimaryNav?: boolean;
  showChangeGroup?: boolean;
}> = ({ children, showPrimaryNav = true, showChangeGroup = true }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar
        showPrimaryNav={showPrimaryNav}
        showChangeGroup={showChangeGroup}
      />
      <div className="max-w-7xl mx-auto px-4 pb-6">
        <main className="flex-1">{children}</main>
      </div>
    </div>
  );
};

export default AppLayout;
