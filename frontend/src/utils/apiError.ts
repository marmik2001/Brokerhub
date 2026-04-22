/**
 * Normalizes errors thrown by fetch/post helpers or runtime exceptions.
 * Returns an object: { message, field } where `field` is a guessed field
 * name (like 'loginId') if the message clearly pertains to a field.
 */
export function parseApiError(err: unknown): {
  message: string;
  field?: string;
} {
  // Default to generic request failed message.
  let message = "Request failed";
  let field: string | undefined = undefined;

  if (!err) {
    return { message, field };
  }

  // If error is an Error instance (thrown by api.ts interceptor).
  if (err instanceof Error) {
    message = err.message || message;
  } else if (typeof err === "string") {
    message = err;
  } else if (typeof err === "object") {
    try {
      message = JSON.stringify(err);
    } catch {
      message = String(err);
    }
  }

  const lc = message.toLowerCase();

  // Heuristics: if message contains loginId or "already taken" for loginId.
  if (
    lc.includes("loginid") ||
    lc.includes("login id") ||
    lc.includes("login id is")
  ) {
    field = "loginId";
  }

  // Also check for common field names.
  if (!field) {
    if (lc.includes("password")) field = "password";
    if (lc.includes("membername") || lc.includes("member name"))
      field = "memberName";
    if (lc.includes("accountname") || lc.includes("account name"))
      field = "accountName";
  }

  return { message, field };
}
