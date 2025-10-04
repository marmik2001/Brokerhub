import React from "react";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

const SelectAccountPage: React.FC = () => {
  const { accounts, selectAccount } = useAuth();
  const navigate = useNavigate();

  const handleSelect = (accountId: string) => {
    selectAccount(accountId);
    navigate("/");
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="bg-white border rounded-lg shadow-sm p-8 w-full max-w-md text-center">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">
          Select a Group to Continue
        </h2>
        <div className="space-y-3">
          {accounts.map((a) => (
            <button
              key={a.accountId}
              onClick={() => handleSelect(a.accountId)}
              className="w-full border border-gray-300 rounded-lg py-2 hover:bg-gray-50 text-gray-700 font-medium"
            >
              Account ID: {a.accountId} ({a.role})
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default SelectAccountPage;
