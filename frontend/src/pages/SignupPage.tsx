import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import * as z from "zod";
import { registerUser } from "../services/authService";
import { parseApiError } from "../utils/apiError";

const signupSchema = z
  .object({
    loginId: z
      .string()
      .min(3, "Login ID must be at least 3 characters")
      .regex(/^[^\s]+$/, "Login ID cannot contain spaces"),
    memberName: z.string().min(2, "Name must be at least 2 characters"),
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

  const [form, setForm] = useState<SignupForm>({
    loginId: "",
    memberName: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [errors, setErrors] = useState<
    Partial<Record<keyof SignupForm, string>>
  >({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleChange =
    (k: keyof SignupForm) => (e: React.ChangeEvent<HTMLInputElement>) => {
      setForm((s) => ({ ...s, [k]: e.target.value }));
      setErrors((f) => ({ ...f, [k]: undefined }));
      setFormError(null);
    };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});
    setFormError(null);

    const parsed = signupSchema.safeParse(form);
    if (!parsed.success) {
      const fieldErrors: any = {};
      for (const issue of parsed.error.issues) {
        fieldErrors[issue.path[0]] = issue.message;
      }
      setErrors(fieldErrors);
      return;
    }

    setSubmitting(true);
    try {
      await registerUser({
        loginId: form.loginId,
        memberName: form.memberName,
        email: form.email,
        password: form.password,
      });

      toast.success("Account created successfully!");
      navigate("/select-account");
    } catch (err: any) {
      const { message } = parseApiError(err);
      setFormError(message || "Signup failed — try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-sm border p-8">
          <h1 className="text-2xl font-semibold text-gray-900 mb-4">Sign Up</h1>
          <p className="text-sm text-gray-600 mb-6">
            Create your personal user account. You can create or join groups
            after login.
          </p>

          {formError && (
            <div className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {formError}
            </div>
          )}

          <form onSubmit={onSubmit} className="space-y-4" noValidate>
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Login ID
              </label>
              <input
                value={form.loginId}
                onChange={handleChange("loginId")}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                required
              />
              {errors.loginId && (
                <p className="text-xs text-red-600 mt-1">{errors.loginId}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Your Name
              </label>
              <input
                value={form.memberName}
                onChange={handleChange("memberName")}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                required
              />
              {errors.memberName && (
                <p className="text-xs text-red-600 mt-1">{errors.memberName}</p>
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
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                placeholder="you@example.com"
              />
              {errors.email && (
                <p className="text-xs text-red-600 mt-1">{errors.email}</p>
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
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                required
              />
              {errors.password && (
                <p className="text-xs text-red-600 mt-1">{errors.password}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Confirm Password
              </label>
              <input
                type="password"
                value={form.confirmPassword}
                onChange={handleChange("confirmPassword")}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                required
              />
              {errors.confirmPassword && (
                <p className="text-xs text-red-600 mt-1">
                  {errors.confirmPassword}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? "Creating account..." : "Sign Up"}
            </button>
          </form>

          <p className="mt-4 text-sm text-gray-600">
            Already registered?{" "}
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
