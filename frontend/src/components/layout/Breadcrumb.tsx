// src/components/layout/Breadcrumb.tsx
import React from "react";
import { useLocation, Link } from "react-router-dom";

const Breadcrumb: React.FC = () => {
  const location = useLocation();
  const paths = location.pathname.split("/").filter(Boolean);

  // Mapping for nicer labels
  const labels: Record<string, string> = {
    "": "Dashboard",
    settings: "Settings",
    profile: "Profile",
    broker: "Broker Access",
    privacy: "Privacy",
    group: "Group Management",
  };

  // Build breadcrumb segments
  const crumbs = paths.length === 0 ? [""] : paths;

  return (
    <div className="bg-gray-50 border-b border-gray-200 px-4 py-3">
      <div className="max-w-7xl mx-auto">
        <nav className="flex items-center gap-2 text-sm text-gray-600">
          {crumbs.map((segment, idx) => {
            const to = "/" + crumbs.slice(0, idx + 1).join("/");
            const isLast = idx === crumbs.length - 1;
            const label = labels[segment] || segment;

            return (
              <React.Fragment key={to}>
                {idx > 0 && <span>/</span>}
                {isLast ? (
                  <span className="text-gray-900 font-medium">{label}</span>
                ) : (
                  <Link to={to} className="hover:underline">
                    {label}
                  </Link>
                )}
              </React.Fragment>
            );
          })}
        </nav>
      </div>
    </div>
  );
};

export default Breadcrumb;
