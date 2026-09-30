"use client";

import { useEffect } from "react";

/**
 * Keeps the screen awake while `active` is true (Screen Wake Lock API).
 * Browsers release the lock when the tab is hidden, so it is re-acquired on return.
 * Calls `onUnsupported` once when the API is missing or the request is refused.
 */
export function useWakeLock(active: boolean, onUnsupported?: () => void) {
  useEffect(() => {
    if (!active) return;

    if (!("wakeLock" in navigator)) {
      onUnsupported?.();
      return;
    }

    let sentinel: WakeLockSentinel | null = null;
    let cancelled = false;

    const request = async () => {
      try {
        const lock = await navigator.wakeLock.request("screen");
        if (cancelled) {
          await lock.release();
          return;
        }
        sentinel = lock;
      } catch {
        onUnsupported?.();
      }
    };

    const handleVisibility = () => {
      if (document.visibilityState === "visible" && !sentinel?.released) {
        void request();
      }
    };

    void request();
    document.addEventListener("visibilitychange", handleVisibility);

    return () => {
      cancelled = true;
      document.removeEventListener("visibilitychange", handleVisibility);
      void sentinel?.release();
      sentinel = null;
    };
    // onUnsupported is intentionally excluded: callers pass inline callbacks
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active]);
}
