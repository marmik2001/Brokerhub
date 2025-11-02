// src/pages/settings/BrokerPage.tsx
import React, { useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import DataTable from "../../components/DataTable";
import AddBrokerCredentialForm from "../../components/AddBrokerCredentialForm";
import {
  listCredentials,
  deleteCredential,
  type BrokerCredential,
} from "../../services/brokerCredentialService";
import { toast } from "react-hot-toast";

const SettingsBrokerPage: React.FC = () => {
  const { currentAccount, isAdmin } = useAuth();

  // credential list state
  const [credentials, setCredentials] = useState<BrokerCredential[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [showAdd, setShowAdd] = useState<boolean>(false);

  // Prefer membership id from currentAccount; no casts needed
  const accountMemberId = currentAccount?.accountMemberId;
  const accountIdFallback = currentAccount?.accountId;

  const load = async () => {
    if (!currentAccount) {
      setCredentials([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await listCredentials(accountMemberId, accountIdFallback);
      setCredentials(data || []);
    } catch (err: any) {
      toast.error(err?.response?.data?.error || "Failed to load credentials");
      setCredentials([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentAccount]);

  const handleDelete = async (id: string) => {
    if (!confirm("Remove this credential?")) return;
    try {
      await deleteCredential(id);
      toast.success("Removed");
      load();
    } catch (err: any) {
      toast.error(err?.response?.data?.error || "Failed to remove");
    }
  };

  const hasCredentials = credentials && credentials.length > 0;

  return (
    <div className="bg-white border rounded p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold">Broker Access</h3>
        {isAdmin && (
          <div>
            <button
              onClick={() => setShowAdd((s) => !s)}
              className="text-sm bg-blue-600 text-white px-3 py-1 rounded-full hover:bg-blue-700"
            >
              + Add
            </button>
          </div>
        )}
      </div>

      {showAdd && isAdmin && (
        <AddBrokerCredentialForm
          accountMemberId={accountMemberId}
          onSaved={() => {
            setShowAdd(false);
            load();
          }}
          onCancel={() => setShowAdd(false)}
        />
      )}

      {loading ? (
        <p className="text-sm text-gray-600">Loading...</p>
      ) : (
        <>
          {!hasCredentials ? (
            isAdmin ? (
              <div className="text-sm text-gray-600 mt-2">
                No broker credentials yet. Use{" "}
                <span className="font-medium">+ Add</span> to add one.
              </div>
            ) : (
              <div className="text-sm text-gray-600 mt-2">
                No broker credentials available.
              </div>
            )
          ) : (
            <DataTable<BrokerCredential>
              data={credentials}
              columns={[
                { header: "Nickname", accessor: (r) => r.nickname },
                { header: "Broker", accessor: (r) => r.broker },
                {
                  header: "Actions",
                  accessor: (r) =>
                    isAdmin ? (
                      <button
                        onClick={() => handleDelete(r.credentialId)}
                        className="text-red-600 hover:underline"
                      >
                        Remove
                      </button>
                    ) : (
                      <span className="text-sm text-gray-500">—</span>
                    ),
                },
              ]}
            />
          )}
        </>
      )}
    </div>
  );
};

export default SettingsBrokerPage;
