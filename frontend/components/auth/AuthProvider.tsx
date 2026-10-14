"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { UserResponse } from "@/types/auth";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: UserResponse | null;
  status: AuthStatus;
}

const AuthContext = createContext<AuthContextValue>({ user: null, status: "loading" });

/**
 * Loads the current user from /api/session on mount, instead of a Server Component
 * reading the cookie. Reading it in a Server Component would force the whole app into
 * dynamic rendering just to show the Navbar's auth state — see app/api/session/route.ts.
 *
 * Consumers should branch on `status`, not just `user`, so the signed-out UI isn't shown
 * for a frame before the real (possibly signed-in) state arrives.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  useEffect(() => {
    let cancelled = false;

    fetch("/api/session", { cache: "no-store" })
      .then((res) => res.json())
      .then((data: { user: UserResponse | null }) => {
        if (cancelled) return;
        setUser(data.user);
        setStatus(data.user ? "authenticated" : "unauthenticated");
      })
      .catch(() => {
        if (!cancelled) setStatus("unauthenticated");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return <AuthContext.Provider value={{ user, status }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
