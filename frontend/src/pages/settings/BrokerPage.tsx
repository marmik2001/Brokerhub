// src/pages/settings/BrokerPage.tsx
import React from "react";

const SettingsBrokerPage: React.FC = () => {
  return (
    <div className="bg-white border rounded p-6">
      <h3 className="text-lg font-semibold mb-4">Broker Access</h3>
      <p className="text-sm text-gray-600">
        Add/manage API tokens for connected brokers. (Mock for now.)
      </p>
    </div>
  );
};

export default SettingsBrokerPage;
