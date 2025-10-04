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
    email: z
      .string()
      .trim()
      .toLowerCase()
      .regex(/^[^\s@]+@[^\s@]+\.[^\s@]+$/, "Invalid email format")
      .optional(),
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
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [fieldErrors, setFieldErrors] = useState<
    Partial<Record<keyof SignupForm, string>>
  >({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleChange =
    (k: keyof SignupForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setForm((s) => ({ ...s, [k]: e.target.value }));
      setFieldErrors((f) => ({ ...f, [k]: undefined }));
      setFormError(null);
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
    setFormError(null);

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
      email: form.email?.toLowerCase() || undefined, // NEW FIELD
      password: form.password,
    };

    setSubmitting(true);

    try {
      await signupService(payload);
      await auth.login(form.loginId, form.password);
      toast.success(`Welcome, ${form.memberName || form.loginId}!`);
      navigate("/");
    } catch (rawErr: any) {
      const { message, field } = parseApiError(rawErr);
      if (
        field &&
        (
          [
            "accountName",
            "accountDesc",
            "loginId",
            "memberName",
            "email",
            "password",
            "confirmPassword",
          ] as (keyof SignupForm)[]
        ).includes(field as keyof SignupForm)
      ) {
        setFieldError(field as keyof SignupForm, message);
      } else {
        setFormError(message || "Signup failed — try again.");
      }
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

          {formError && (
            <div
              role="alert"
              className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
            >
              {formError}
            </div>
          )}

          <form onSubmit={onSubmit} className="space-y-4" noValidate>
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Account name
              </label>
              <input
                value={form.accountName}
                onChange={handleChange("accountName")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.accountName ? "border-red-300" : "border-gray-300"
                }`}
                required
              />
              {fieldErrors.accountName && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.accountName}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Account description (optional)
              </label>
              <textarea
                value={form.accountDesc}
                onChange={handleChange("accountDesc")}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={2}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Login ID
              </label>
              <input
                value={form.loginId}
                onChange={handleChange("loginId")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.loginId ? "border-red-300" : "border-gray-300"
                }`}
                required
              />
              {fieldErrors.loginId && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.loginId}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Your name
              </label>
              <input
                value={form.memberName}
                onChange={handleChange("memberName")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.memberName ? "border-red-300" : "border-gray-300"
                }`}
                required
              />
              {fieldErrors.memberName && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.memberName}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Email
              </label>
              <input
                type="email"
                value={form.email}
                onChange={handleChange("email")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.email ? "border-red-300" : "border-gray-300"
                }`}
                placeholder="you@example.com"
              />
              {fieldErrors.email && (
                <p className="text-xs text-red-600 mt-1">{fieldErrors.email}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Password
              </label>
              <input
                type="password"
                value={form.password}
                onChange={handleChange("password")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.password ? "border-red-300" : "border-gray-300"
                }`}
                required
              />
              {fieldErrors.password && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.password}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Confirm password
              </label>
              <input
                type="password"
                value={form.confirmPassword}
                onChange={handleChange("confirmPassword")}
                className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  fieldErrors.confirmPassword
                    ? "border-red-300"
                    : "border-gray-300"
                }`}
                required
              />
              {fieldErrors.confirmPassword && (
                <p className="text-xs text-red-600 mt-1">
                  {fieldErrors.confirmPassword}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? "Creating account…" : "Sign up"}
            </button>
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
