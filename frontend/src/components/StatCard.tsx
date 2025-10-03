// src/components/StatCard.tsx
import React from "react";

const StatCard: React.FC<{
  title: string;
  value: string | number;
  sub?: string;
}> = ({ title, value, sub }) => {
  return (
    <div className="bg-white rounded-lg shadow-sm border p-4">
      <div className="text-xs text-gray-500">{title}</div>
      <div className="mt-2 text-xl font-semibold text-gray-900">{value}</div>
      {sub && <div className="text-sm text-gray-500 mt-1">{sub}</div>}
    </div>
  );
};

export default StatCard;
