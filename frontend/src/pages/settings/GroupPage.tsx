import React, { useEffect, useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import {
  getMembers,
  addMember,
  updateMemberRole,
  removeMember,
  type Member,
} from "../../services/accountService";
import { toast } from "react-hot-toast";
import DataTable from "../../components/DataTable";

const GroupPage: React.FC = () => {
  const { currentAccount, isAdmin } = useAuth();
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [loginIdOrEmail, setLoginIdOrEmail] = useState("");

  useEffect(() => {
    if (!currentAccount) return;
    loadMembers();
  }, [currentAccount]);

  const loadMembers = async () => {
    try {
      if (!currentAccount) return;
      setLoading(true);
      const data = await getMembers(currentAccount.accountId);
      setMembers(data);
    } catch {
      toast.error("Failed to load members");
    } finally {
      setLoading(false);
    }
  };

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginIdOrEmail.trim() || !currentAccount) return;
    try {
      await addMember(currentAccount.accountId, {
        loginId: loginIdOrEmail,
        email: loginIdOrEmail.includes("@") ? loginIdOrEmail : undefined,
      });
      toast.success("Member added successfully");
      setLoginIdOrEmail("");
      loadMembers();
    } catch (err: any) {
      toast.error(err?.response?.data?.error || "Failed to add member");
    }
  };

  const handleRoleChange = async (
    memberId: string,
    newRole: "ADMIN" | "MEMBER"
  ) => {
    if (!currentAccount) return;
    try {
      await updateMemberRole(currentAccount.accountId, memberId, newRole);
      toast.success("Role updated");
      loadMembers();
    } catch {
      toast.error("Failed to update role");
    }
  };

  const handleRemove = async (memberId: string) => {
    if (!currentAccount) return;
    if (!confirm("Remove this member?")) return;
    try {
      await removeMember(currentAccount.accountId, memberId);
      toast.success("Member removed");
      loadMembers();
    } catch {
      toast.error("Failed to remove member");
    }
  };

  return (
    <div className="bg-white border rounded-lg p-6">
      <h3 className="text-lg font-semibold mb-4">Group Management</h3>

      {isAdmin && (
        <form
          onSubmit={handleAddMember}
          className="flex flex-col sm:flex-row gap-3 mb-6"
        >
          <input
            type="text"
            placeholder="Enter login ID or email"
            value={loginIdOrEmail}
            onChange={(e) => setLoginIdOrEmail(e.target.value)}
            className="flex-1 border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
            required
          />
          <button
            type="submit"
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
          >
            Add Member
          </button>
        </form>
      )}

      {loading ? (
        <p className="text-gray-600 text-sm">Loading members...</p>
      ) : (
        <DataTable<Member>
          data={members}
          columns={[
            { header: "Name", accessor: (m) => m.memberName },
            { header: "Login ID", accessor: (m) => m.loginId },
            { header: "Email", accessor: (m) => m.email || "—" },
            {
              header: "Role",
              accessor: (m) =>
                isAdmin ? (
                  <select
                    value={m.role}
                    onChange={(e) =>
                      handleRoleChange(
                        m.memberId,
                        e.target.value as "ADMIN" | "MEMBER"
                      )
                    }
                    className="border border-gray-300 rounded px-2 py-1"
                    disabled={m.role === "ADMIN" && m.loginId === "self"}
                  >
                    <option value="ADMIN">ADMIN</option>
                    <option value="MEMBER">MEMBER</option>
                  </select>
                ) : (
                  m.role
                ),
            },
            ...(isAdmin
              ? [
                  {
                    header: "Actions",
                    accessor: (m: Member) => (
                      <button
                        onClick={() => handleRemove(m.memberId)}
                        className="text-red-600 hover:underline"
                      >
                        Remove
                      </button>
                    ),
                  },
                ]
              : []),
          ]}
        />
      )}
    </div>
  );
};

export default GroupPage;
