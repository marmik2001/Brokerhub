import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import * as z from "zod";
import { createAccount } from "../services/accountService";
import { useAuth } from "../contexts/AuthContext";
import { parseApiError } from "../utils/apiError";

const schema = z.object({
  accountName: z.string().min(2, "Account name must be at least 2 characters"),
  accountDesc: z.string().optional(),
});

type Form = z.infer<typeof schema>;

const CreateAccountPage: React.FC = () => {
  const [form, setForm] = useState<Form>({ accountName: "", accountDesc: "" });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { addAccount } = useAuth();

  const handleChange =
    (k: keyof Form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setForm((s) => ({ ...s, [k]: e.target.value }));
      setError(null);
    };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const parsed = schema.safeParse(form);
    if (!parsed.success) {
      setError(parsed.error.issues[0]?.message || "Invalid input");
      return;
    }

    setSubmitting(true);
    try {
      const newAccount = await createAccount(form);
      addAccount(newAccount);
      toast.success("Group created successfully!");
      navigate("/");
    } catch (err: any) {
      const { message } = parseApiError(err);
      setError(message || "Failed to create group");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="bg-white border rounded-lg shadow-sm p-8 w-full max-w-md text-center">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">
          Create a New Group
        </h2>

        {error && (
          <div className="mb-4 text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={onSubmit} className="space-y-4 text-left">
          <div>
            <label className="block text-sm text-gray-700 mb-1">
              Group Name
            </label>
            <input
              value={form.accountName}
              onChange={handleChange("accountName")}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              required
            />
          </div>

          <div>
            <label className="block text-sm text-gray-700 mb-1">
              Description (optional)
            </label>
            <textarea
              value={form.accountDesc}
              onChange={handleChange("accountDesc")}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg"
              rows={2}
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? "Creating..." : "Create Group"}
          </button>
        </form>
      </div>
    </div>
  );
};

export default CreateAccountPage;
