import React, { useState } from "react";
import { useAuth } from "../../contexts/AuthContext";
import { changePassword } from "../../services/authService";
import { toast } from "react-hot-toast";

const ProfilePage: React.FC = () => {
  const { user } = useAuth();
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handlePasswordChange = async () => {
    if (newPassword !== confirmPassword) {
      toast.error("New passwords do not match");
      return;
    }

    try {
      setLoading(true);
      const res = await changePassword({ oldPassword, newPassword });
      toast.success(res.message || "Password updated successfully");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Error changing password";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white border rounded p-6">
      <h3 className="text-lg font-semibold mb-6">Profile</h3>

      {/* Profile Info */}
      <dl className="divide-y divide-gray-100 text-sm mb-8">
        <div className="py-3 flex justify-between">
          <dt className="font-medium text-gray-600">Name</dt>
          <dd className="text-gray-900">{user?.name}</dd>
        </div>
        <div className="py-3 flex justify-between">
          <dt className="font-medium text-gray-600">Login ID</dt>
          <dd className="text-gray-900">{user?.loginId}</dd>
        </div>
        <div className="py-3 flex justify-between">
          <dt className="font-medium text-gray-600">Role</dt>
          <dd className="text-gray-900">{user?.role}</dd>
        </div>
      </dl>

      {/* Change Password */}
      <h4 className="text-md font-semibold text-gray-900 mb-4">
        Change Password
      </h4>
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Current Password
          </label>
          <input
            type="password"
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            placeholder="••••••••"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            New Password
          </label>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            placeholder="••••••••"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Confirm New Password
          </label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none"
            placeholder="••••••••"
          />
        </div>

        <button
          onClick={handlePasswordChange}
          disabled={loading}
          className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition"
        >
          {loading ? "Updating..." : "Update Password"}
        </button>
      </div>
    </div>
  );
};

export default ProfilePage;
