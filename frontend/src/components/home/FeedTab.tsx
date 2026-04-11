import React from "react";
import EmptyState from "../EmptyState";

const FeedTab: React.FC = () => {
  return (
    <div className="bg-white border rounded p-6">
      <EmptyState
        title="Coming soon..."
        description="Activity feed & notifications will be added here."
      />
    </div>
  );
};

export default FeedTab;
