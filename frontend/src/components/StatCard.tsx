import React from "react";

type Props = {
  title: string;
  value: React.ReactNode; // now accepts ReactNode for richer content
  description?: string;
  className?: string;
};

const StatCard: React.FC<Props> = ({
  title,
  value,
  description,
  className = "",
}) => {
  return (
    <div className={`bg-white border rounded-lg p-4 shadow-sm ${className}`}>
      <div className="flex items-center justify-between">
        <div className="text-xs font-medium text-gray-500">{title}</div>
      </div>

      <div className="mt-3">
        <div className="text-xl font-semibold text-gray-900">{value}</div>
        {description && (
          <div className="mt-1 text-sm text-gray-500">{description}</div>
        )}
      </div>
    </div>
  );
};

export default StatCard;
