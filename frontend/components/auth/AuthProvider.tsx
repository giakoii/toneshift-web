"use client";

import { createContext, useContext, useState, type ReactNode } from "react";
import type { UserResponse } from "@/types/auth";

interface AuthContextValue {
  user: UserResponse | null;
}

const AuthContext = createContext<AuthContextValue>({ user: null });

export function AuthProvider({
  initialUser,
  children,
}: {
  initialUser: UserResponse | null;
  children: ReactNode;
}) {
  // Không cần setState phức tạp ở core scope: logout/login đều redirect + full reload
  // dữ liệu session qua Server Component cha, nên initialUser luôn đúng sau mỗi điều hướng.
  const [user] = useState(initialUser);
  return <AuthContext.Provider value={{ user }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
