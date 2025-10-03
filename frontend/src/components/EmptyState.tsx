// src/components/EmptyState.tsx
import React from "react";

const EmptyState: React.FC<{ title?: string; description?: string }> = ({
  title = "No data",
  description,
}) => {
  return (
    <div className="p-8 text-center text-gray-600">
      <div className="text-lg font-medium">{title}</div>
      {description && <div className="mt-2">{description}</div>}
    </div>
  );
};

export default EmptyState;
