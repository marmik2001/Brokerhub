// src/components/layout/Breadcrumb.tsx
import React from "react";
import { useLocation, Link } from "react-router-dom";
import { ROUTE_LABELS } from "../../routes/routeLabels.ts";

const Breadcrumb: React.FC = () => {
  const location = useLocation();
  const paths = location.pathname.split("/").filter(Boolean);

  if (paths.length === 0) {
    return (
      <div className="bg-gray-50 border-b border-gray-200 px-4 py-3">
        <div className="max-w-7xl mx-auto">
          <div className="text-sm text-gray-600">Home</div>
        </div>
      </div>
    );
  }

  const crumbs = paths.map((p, idx) => {
    const to = "/" + paths.slice(0, idx + 1).join("/");
    return {
      label: ROUTE_LABELS[p] ?? p.charAt(0).toUpperCase() + p.slice(1),
      to,
    };
  });

  return (
    <div className="bg-gray-50 border-b border-gray-200 px-4 py-3">
      <div className="max-w-7xl mx-auto">
        <div className="flex items-center gap-2 text-sm text-gray-600">
          {crumbs.map((c, i) => (
            <React.Fragment key={i}>
              {i > 0 && <span>/</span>}
              {i < crumbs.length - 1 ? (
                <Link to={c.to} className="text-gray-600 hover:underline">
                  {c.label}
                </Link>
              ) : (
                <span className="text-gray-900 font-medium">{c.label}</span>
              )}
            </React.Fragment>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Breadcrumb;
