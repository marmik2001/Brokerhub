import React, { useEffect, useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import api from "../api";
import type { AccountSummary } from "../services/authService";

const SelectGroupPage: React.FC = () => {
  // NOTE: we intentionally do NOT rely on auth.accounts here.
  // This page fetches the authoritative accounts list from GET /api/accounts.
  const { selectAccountDirect } = useAuth();
  const navigate = useNavigate();

  const [accounts, setAccounts] = useState<AccountSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      try {
        const resp = await api.get("/accounts");
        // expected payload: [{ accountId, name, description, role, accountMemberId }]
        if (!mounted) return;
        setAccounts(resp.data || []);
      } catch (e: any) {
        console.error("Failed to fetch accounts", e);
        if (mounted) setErr(e?.message || "Failed to load accounts");
      } finally {
        if (mounted) setLoading(false);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const handleSelect = (account: AccountSummary) => {
    // Persist selection via AuthContext helper which will:
    //  - persist currentAccount into localStorage
    //  - if accountMemberId not present, attempt to fetch it in background
    selectAccountDirect(account);
    navigate("/");
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="bg-white border rounded-lg shadow-sm p-8 w-full max-w-md text-center">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">
          Select a Group to Continue
        </h2>

        {loading ? (
          <div className="text-sm text-gray-600">Loading accounts...</div>
        ) : err ? (
          <div className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded">
            {err}
          </div>
        ) : (
          <div className="space-y-3">
            {accounts.length === 0 && (
              <div className="text-sm text-gray-600">No accounts found</div>
            )}

            {accounts.map((a) => (
              <button
                key={a.accountId}
                onClick={() => handleSelect(a)}
                className="w-full p-4 rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-all text-left bg-white hover:bg-gray-50"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-gray-900 font-semibold text-sm">
                      {a.name ?? a.accountId}
                    </div>
                    <div className="text-xs text-gray-500 mt-0.5">
                      Role: {a.role ?? "MEMBER"}
                    </div>
                  </div>

                  <div className="text-gray-400">➜</div>
                </div>
              </button>
            ))}

            <button
              onClick={() => navigate("/create-account")}
              className="w-full mt-4 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700"
            >
              + Add Group
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default SelectGroupPage;
