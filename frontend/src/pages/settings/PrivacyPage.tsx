import React, { useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { updateMemberRule } from "../../services/authService";
import { toast } from "react-hot-toast";
import { parseApiError } from "../../utils/apiError";

const SettingsPrivacyPage: React.FC = () => {
  const { currentAccount, updateAccountRules } = useAuth();

  const initialPrivacy =
    (currentAccount &&
      (currentAccount.rules?.privacy ??
        (typeof currentAccount.rules === "string"
          ? (() => {
              try {
                return JSON.parse(currentAccount.rules).privacy;
              } catch {
                return undefined;
              }
            })()
          : undefined))) ||
    "DETAILED";

  const [privacy, setPrivacy] = useState<"DETAILED" | "SUMMARY" | "PRIVATE">(
    initialPrivacy as any
  );
  const [submitting, setSubmitting] = useState(false);

  if (!currentAccount) {
    return (
      <div className="bg-white border rounded p-6">
        <h3 className="text-lg font-semibold mb-6">Privacy</h3>
        <p className="text-sm text-gray-600">
          No account selected. Please select an account to manage privacy.
        </p>
      </div>
    );
  }

  if (!currentAccount.accountMemberId) {
    return (
      <div className="bg-white border rounded p-6">
        <h3 className="text-lg font-semibold mb-6">Privacy</h3>
        <p className="text-sm text-gray-600">
          Unable to edit privacy for this account (missing membership id).
        </p>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!["DETAILED", "SUMMARY", "PRIVATE"].includes(privacy)) {
      toast.error("Invalid privacy selection");
      return;
    }
    setSubmitting(true);
    try {
      const resp = await updateMemberRule(
        currentAccount.accountId,
        currentAccount.accountMemberId,
        privacy
      );

      // The response rules may be a raw JSON string or an object.
      let parsedRules: any = resp.rules;
      if (typeof parsedRules === "string") {
        try {
          parsedRules = JSON.parse(parsedRules);
        } catch {
          // Leave as string if parsing fails.
        }
      }

      // Update auth context so UI reflects new rules.
      updateAccountRules(currentAccount.accountId, parsedRules);

      toast.success("Privacy updated");
    } catch (err: unknown) {
      const { message } = parseApiError(err);
      toast.error(message || "Failed to update privacy");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="bg-white border rounded p-6">
      <h3 className="text-lg font-semibold mb-6">Privacy</h3>
      <p className="text-sm text-gray-600 mb-6">
        Choose what others can see for this account's holdings and positions.
      </p>

      <form onSubmit={handleSubmit} className="w-full">
        <div className="space-y-4 mb-6">
          <label className="flex items-start gap-3 p-4 border rounded-lg hover:bg-gray-50">
            <input
              type="radio"
              name="privacy"
              value="DETAILED"
              checked={privacy === "DETAILED"}
              onChange={() => setPrivacy("DETAILED")}
              className="mt-1"
            />
            <div>
              <div className="font-medium">DETAILED</div>
              <div className="text-sm text-gray-600">
                Show ticker, quantity, buy price and P&L to other account
                members.
              </div>
            </div>
          </label>

          <label className="flex items-start gap-3 p-4 border rounded-lg hover:bg-gray-50">
            <input
              type="radio"
              name="privacy"
              value="SUMMARY"
              checked={privacy === "SUMMARY"}
              onChange={() => setPrivacy("SUMMARY")}
              className="mt-1"
            />
            <div>
              <div className="font-medium">SUMMARY</div>
              <div className="text-sm text-gray-600">
                Show only the ticker symbol (no quantities or prices).
              </div>
            </div>
          </label>

          <label className="flex items-start gap-3 p-4 border rounded-lg hover:bg-gray-50">
            <input
              type="radio"
              name="privacy"
              value="PRIVATE"
              checked={privacy === "PRIVATE"}
              onChange={() => setPrivacy("PRIVATE")}
              className="mt-1"
            />
            <div>
              <div className="font-medium">PRIVATE</div>
              <div className="text-sm text-gray-600">
                Hide this account’s holdings from other account members.
              </div>
            </div>
          </label>
        </div>

        <div>
          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
            disabled={submitting}
          >
            {submitting ? "Saving..." : "Save"}
          </button>
        </div>
      </form>
    </div>
  );
};

export default SettingsPrivacyPage;
