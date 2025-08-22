// src/pages/ZerodhaCallback.tsx
import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import API from "../api";

const ZerodhaCallback = () => {
  const [params] = useSearchParams();
  const [status, setStatus] = useState("Processing...");
  const navigate = useNavigate();

  useEffect(() => {
    const requestToken = params.get("request_token");
    if (requestToken) {
      API.post(`/generate-token?requestToken=${requestToken}`)
        .then(() => {
          setStatus("Login successful! Redirecting...");
          setTimeout(() => navigate("/portfolio"), 1000);
        })
        .catch(() => setStatus("Error generating access token."));
    } else {
      setStatus("No request token in URL.");
    }
  }, [params, navigate]);

  return (
    <div className="flex h-screen items-center justify-center bg-gray-50">
      <div className="bg-white p-6 rounded-xl shadow text-gray-700">
        <h2 className="text-lg font-medium">{status}</h2>
      </div>
    </div>
  );
};

export default ZerodhaCallback;
