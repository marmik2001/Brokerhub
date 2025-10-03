// src/pages/settings/PrivacyPage.tsx
import React from "react";

const SettingsPrivacyPage: React.FC = () => {
  return (
    <div className="bg-white border rounded p-6">
      <h3 className="text-lg font-semibold mb-4">Privacy</h3>
      <p className="text-sm text-gray-600">
        Choose what others can see: holdings, positions, P&L %, etc. (Mock.)
      </p>
    </div>
  );
};

export default SettingsPrivacyPage;
