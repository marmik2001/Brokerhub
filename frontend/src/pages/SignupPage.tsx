// src/pages/SignupPage.tsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import * as z from "zod";
import {
  signup as signupService,
  type SignupRequest,
} from "../services/accountService";
import { useAuth } from "../contexts/AuthContext";
import { parseApiError } from "../utils/apiError";

// zod schema with refine for confirm password
const signupSchema = z
  .object({
    accountName: z
      .string()
      .min(2, "Account name must be at least 2 characters"),
    accountDesc: z.string().optional(),
    loginId: z
      .string()
      .min(3, "Login ID must be at least 3 characters")
      .regex(/^[^\s]+$/, "Login ID cannot contain spaces"),
    memberName: z.string().min(2, "Member name must be at least 2 characters"),
    password: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ["confirmPassword"],
    message: "Passwords do not match",
  });

type SignupForm = z.infer<typeof signupSchema>;

const SignupPage: React.FC = () => {
  const navigate = useNavigate();
  const auth = useAuth();

  const [form, setForm] = useState<SignupForm>({
    accountName: "",
    accountDesc: "",
    loginId: "",
    memberName: "",
    password: "",
    confirmPassword: "",
  });

  const [fieldErrors, setFieldErrors] = useState<
    Partial<Record<keyof SignupForm, string>>
  >({});
  const [submitting, setSubmitting] = useState(false);

  const handleChange =
    (k: keyof SignupForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setForm((s) => ({ ...s, [k]: e.target.value }));
      setFieldErrors((f) => ({ ...f, [k]: undefined }));
    };

  const setFieldError = (field: keyof SignupForm, msg: string) => {
    setFieldErrors((prev) => ({ ...prev, [field]: msg }));
  };

  const clearPasswordFields = () => {
    setForm((s) => ({ ...s, password: "", confirmPassword: "" }));
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFieldErrors({});
    // client-side validation
    const parsed = signupSchema.safeParse(form);
    if (!parsed.success) {
      const zErrors: Partial<Record<keyof SignupForm, string>> = {};
      for (const issue of parsed.error.issues) {
        const key = issue.path[0] as keyof SignupForm | undefined;
        if (key) zErrors[key] = issue.message;
      }
      setFieldErrors(zErrors);
      return;
    }

    const payload: SignupRequest = {
      accountName: form.accountName,
      accountDesc: form.accountDesc || undefined,
      loginId: form.loginId,
      memberName: form.memberName,
      password: form.password,
    };

    setSubmitting(true);

    try {
      await signupService(payload);
      // auto-login
      await auth.login(form.loginId, form.password);

      toast.success(
        `Welcome, ${form.memberName || form.loginId}! Your account is ready.`
      );
      navigate("/");
    } catch (rawErr: any) {
      const { message, field } = parseApiError(rawErr);
      // set inline field error for loginId if applicable
      if (field === "loginId") {
        setFieldError("loginId", message);
      }
      // show toast for the error
      toast.error(message || "Signup failed — try again");
      // clear sensitive fields
      clearPasswordFields();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-lg">
        <div className="bg-white rounded-lg shadow-sm border p-8">
          <h1 className="text-2xl font-semibold text-gray-900 mb-4">
            Create a BrokerHub account
          </h1>
          <p className="text-sm text-gray-600 mb-6">
            Create a family/group account and become the Admin. Login ID must be
            unique.
          </p>

          <form
            onSubmit={onSubmit}
            className="space-y-4"
            aria-labelledby="signup-form"
          >
            <div>
              <label
                htmlFor="accountName"
                className="block text-sm font-medium text-gray-700"
              >
                Account name
              </label>
              <input
                id="accountName"
                value={form.accountName}
                onChange={handleChange("accountName")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.accountName ? "border-red-300" : "border-gray-300"
                }`}
                aria-invalid={!!fieldErrors.accountName}
                required
              />
              {fieldErrors.accountName && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.accountName}
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="accountDesc"
                className="block text-sm font-medium text-gray-700"
              >
                Account description (optional)
              </label>
              <textarea
                id="accountDesc"
                value={form.accountDesc}
                onChange={handleChange("accountDesc")}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={2}
              />
            </div>

            <div>
              <label
                htmlFor="loginId"
                className="block text-sm font-medium text-gray-700"
              >
                Login ID
              </label>
              <input
                id="loginId"
                value={form.loginId}
                onChange={handleChange("loginId")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.loginId ? "border-red-300" : "border-gray-300"
                }`}
                aria-invalid={!!fieldErrors.loginId}
                aria-describedby={
                  fieldErrors.loginId ? "loginId-error" : undefined
                }
                required
              />
              {fieldErrors.loginId && (
                <p id="loginId-error" className="text-xs text-red-600 mt-1">
                  {fieldErrors.loginId}
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="memberName"
                className="block text-sm font-medium text-gray-700"
              >
                Your name
              </label>
              <input
                id="memberName"
                value={form.memberName}
                onChange={handleChange("memberName")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.memberName ? "border-red-300" : "border-gray-300"
                }`}
                aria-invalid={!!fieldErrors.memberName}
                required
              />
              {fieldErrors.memberName && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.memberName}
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700"
              >
                Password
              </label>
              <input
                id="password"
                type="password"
                value={form.password}
                onChange={handleChange("password")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.password ? "border-red-300" : "border-gray-300"
                }`}
                aria-invalid={!!fieldErrors.password}
                required
              />
              {fieldErrors.password && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.password}
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="confirmPassword"
                className="block text-sm font-medium text-gray-700"
              >
                Confirm password
              </label>
              <input
                id="confirmPassword"
                type="password"
                value={form.confirmPassword}
                onChange={handleChange("confirmPassword")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.confirmPassword
                    ? "border-red-300"
                    : "border-gray-300"
                }`}
                aria-invalid={!!fieldErrors.confirmPassword}
                required
              />
              {fieldErrors.confirmPassword && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.confirmPassword}
                </p>
              )}
            </div>

            <div>
              <button
                type="submit"
                disabled={submitting}
                aria-busy={submitting}
                className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {submitting ? "Creating account…" : "Sign up"}
              </button>
            </div>
          </form>

          <p className="mt-4 text-sm text-gray-600">
            Already have an account?{" "}
            <button
              onClick={() => navigate("/login")}
              className="text-blue-600 hover:underline"
            >
              Log in
            </button>
          </p>
        </div>
      </div>
    </div>
  );
};

export default SignupPage;
