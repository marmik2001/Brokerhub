// src/utils/apiError.ts
/**
 * Normalize errors thrown by fetch/post helpers or runtime exceptions.
 * Returns an object: { message, field } where `field` is a guessed field
 * name (like 'loginId') if the message clearly pertains to a field.
 */

export function parseApiError(err: unknown): {
  message: string;
  field?: string;
} {
  // Default
  let message = "Request failed";
  let field: string | undefined = undefined;

  if (!err) {
    return { message, field };
  }

  // If error is an Error instance
  if (err instanceof Error) {
    message = err.message || message;
  } else if (typeof err === "string") {
    message = err;
  } else if (typeof err === "object") {
    // try to extract common keys
    const anyErr = err as any;
    if (typeof anyErr.error === "string") {
      message = anyErr.error;
    } else if (typeof anyErr.message === "string") {
      message = anyErr.message;
    } else {
      try {
        message = JSON.stringify(anyErr);
      } catch {
        message = String(anyErr);
      }
    }
  }

  const lc = message.toLowerCase();

  // heuristics: if message contains loginid or login id or "already taken" for loginId
  if (
    lc.includes("loginid") ||
    lc.includes("login id") ||
    lc.includes("login id is")
  ) {
    field = "loginId";
  }

  // also check for common field names
  if (!field) {
    if (lc.includes("password")) field = "password";
    if (lc.includes("membername") || lc.includes("member name"))
      field = "memberName";
    if (lc.includes("accountname") || lc.includes("account name"))
      field = "accountName";
  }

  return { message, field };
}
