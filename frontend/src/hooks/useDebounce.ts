import { useEffect, useState } from "react";

/**
 * useDebounce - returns a debounced value updated after `delay` ms of idle time.
 *
 * Usage:
 * const debounced = useDebounce(value, 300);
 */
export function useDebounce<T>(value: T, delay = 300) {
  const [debounced, setDebounced] = useState<T>(value);

  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(id);
  }, [value, delay]);

  return debounced;
}

export default useDebounce;
