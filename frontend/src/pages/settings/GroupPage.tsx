// src/pages/settings/GroupPage.tsx
import React from "react";

const SettingsGroupPage: React.FC = () => {
  return (
    <div className="bg-white border rounded p-6">
      <h3 className="text-lg font-semibold mb-4">Group Management</h3>
      <p className="text-sm text-gray-600">
        Invite/remove members and assign roles (Admin / Member). (Mock.)
      </p>
    </div>
  );
};

export default SettingsGroupPage;
