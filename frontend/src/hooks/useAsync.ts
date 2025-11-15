import { useEffect, useRef, useState, useCallback } from "react";

/**
 * useAsync - lightweight async helper
 *
 * @param factory - a function that returns a Promise<T>
 * @param deps - dependency list to re-run the factory
 * @param autoRun - whether to run automatically on mount / deps change (default: true)
 *
 * Returns { data, loading, error, refetch }.
 *
 * Notes:
 * - avoids setting state after unmount
 * - ignores out-of-order promise results (stale promise handling)
 */
export function useAsync<T>(
  factory: () => Promise<T>,
  deps: any[] = [],
  autoRun = true
) {
  const mountedRef = useRef(true);
  const promiseIdRef = useRef(0);

  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState<boolean>(!!autoRun);
  const [error, setError] = useState<any>(null);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const run = useCallback(async () => {
    const myId = ++promiseIdRef.current;
    setLoading(true);
    setError(null);
    try {
      const res = await factory();
      // ignore if a newer promise started
      if (!mountedRef.current || myId !== promiseIdRef.current) return;
      setData(res);
      setLoading(false);
      return res;
    } catch (err) {
      if (!mountedRef.current || myId !== promiseIdRef.current) return;
      setError(err);
      setLoading(false);
      throw err;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  // auto-run when deps change
  useEffect(() => {
    if (autoRun) {
      // fire and ignore unhandled promise here (caller can use refetch)
      // but we keep internal state updated
      run().catch(() => {
        /* swallow - state is set in run */
      });
    }
    // run and its dependencies are provided by deps / callback
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  const refetch = useCallback(() => {
    return run();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, loading, error, refetch };
}
