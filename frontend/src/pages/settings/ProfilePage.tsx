// src/pages/settings/ProfilePage.tsx
import React from "react";
import { useAuth } from "../../contexts/AuthContext";

const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="bg-white border rounded p-6">
      <h3 className="text-lg font-semibold mb-4">Profile</h3>
      <div className="space-y-3">
        <div>
          <strong>Name:</strong> {user?.name}
        </div>
        <div>
          <strong>Login ID:</strong> {user?.loginId}
        </div>
        <div>
          <strong>Role:</strong> {user?.role}
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
