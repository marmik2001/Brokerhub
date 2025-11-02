// src/components/AddBrokerCredentialForm.tsx
import React, { useState } from "react";
import { toast } from "react-hot-toast";
import { storeCredential } from "../services/brokerCredentialService";

interface Props {
  accountMemberId?: string; // required to create; parent may pass it
  onSaved: () => void;
  onCancel?: () => void;
}

/**
 * Simple inline form for adding a broker credential.
 * Note: never persists or displays the plaintext token after submission.
 */
const AddBrokerCredentialForm: React.FC<Props> = ({
  accountMemberId,
  onSaved,
  onCancel,
}) => {
  const [broker, setBroker] = useState<string>("DHAN");
  const [nickname, setNickname] = useState<string>("");
  const [tokenJson, setTokenJson] = useState<string>("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!accountMemberId) {
      toast.error(
        "Missing membership ID (accountMemberId). Please re-select the group."
      );
      return;
    }
    if (!nickname.trim()) {
      toast.error("Nickname is required");
      return;
    }
    if (!tokenJson.trim()) {
      toast.error("Access token is required");
      return;
    }

    setLoading(true);
    try {
      await storeCredential({
        accountMemberId,
        broker,
        nickname: nickname.trim(),
        token: tokenJson.trim(),
      });
      toast.success("Saved");
      // wipe token input immediately (do not store)
      setTokenJson("");
      onSaved();
    } catch (err: any) {
      const msg =
        err?.response?.data?.error || err?.message || "Failed to save";
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3 border-b pb-4 mb-4">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-semibold">Add Broker Credential</h4>
        <div className="text-xs text-gray-500">
          You can add credentials for this membership
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div>
          <label className="block text-xs text-gray-600 mb-1">Broker</label>
          <select
            value={broker}
            onChange={(e) => setBroker(e.target.value)}
            className="w-full border rounded px-2 py-2"
            required
          >
            <option value="DHAN">DHAN</option>
          </select>
        </div>

        <div className="sm:col-span-2">
          <label className="block text-xs text-gray-600 mb-1">Nickname</label>
          <input
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            className="w-full border rounded px-3 py-2"
            placeholder="e.g. My DHAN account"
            required
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700">
          Access Token
        </label>
        <textarea
          value={tokenJson}
          onChange={(e) => setTokenJson(e.target.value)}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
          placeholder="Paste your broker access token here"
          required
        />
      </div>

      <div className="flex gap-2">
        <button
          type="submit"
          disabled={loading}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? "Saving..." : "Save"}
        </button>
        <button
          type="button"
          onClick={() => {
            setTokenJson("");
            setNickname("");
            onCancel?.();
          }}
          className="px-3 py-2 rounded border text-gray-700 hover:bg-gray-50"
        >
          Cancel
        </button>
      </div>
    </form>
  );
};

export default AddBrokerCredentialForm;
