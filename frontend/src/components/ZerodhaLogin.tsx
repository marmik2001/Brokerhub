// src/components/Login.tsx
import { useEffect, useState } from "react";
import KITE_API from "../api";

const Login = () => {
  const [loginUrl, setLoginUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchLoginUrl = async () => {
      try {
        const response = await KITE_API.get("/login-url");
        setLoginUrl(response.data); // backend should return the Zerodha login URL as string
      } catch (err) {
        setError("Failed to load login URL");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchLoginUrl();
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center text-gray-600">
        Loading...
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-screen items-center justify-center text-red-600">
        {error}
      </div>
    );
  }

  return (
    <div className="flex h-screen items-center justify-center bg-gray-50">
      <div className="bg-white p-8 rounded-2xl shadow-md w-80 text-center">
        <h1 className="text-2xl font-semibold mb-6">BrokerHub</h1>
        <a
          href={loginUrl || "#"}
          className="block w-full bg-blue-600 text-white py-2 px-4 rounded-xl hover:bg-blue-700 transition-colors"
        >
          Login with Zerodha
        </a>
      </div>
    </div>
  );
};

export default Login;
