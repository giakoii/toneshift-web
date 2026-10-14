"use server";

import { redirect } from "next/navigation";
import { apiFetch, ApiError } from "@/lib/api-client";
import { createSession, destroySession, getSession } from "@/lib/session";
import type { AuthResponse, FieldErrorItem } from "@/types/auth";

export interface AuthFormState {
  error?: string;
  fieldErrors?: FieldErrorItem[];
}

export async function registerAction(
  _prevState: AuthFormState,
  formData: FormData
): Promise<AuthFormState> {
  const name = String(formData.get("name") ?? "").trim();
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  try {
    const auth = await apiFetch<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password }),
    });
    await createSession(auth);
  } catch (e) {
    return toFormState(e);
  }
  redirect("/");
}

export async function loginAction(
  _prevState: AuthFormState,
  formData: FormData
): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  try {
    const auth = await apiFetch<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    await createSession(auth);
  } catch (e) {
    return toFormState(e);
  }
  redirect("/");
}

// Gọi trực tiếp từ GoogleButton (không phải form submit, nên không dùng useActionState ở đây)
export async function googleLoginAction(idToken: string): Promise<AuthFormState> {
  try {
    const auth = await apiFetch<AuthResponse>("/api/auth/oauth/google", {
      method: "POST",
      body: JSON.stringify({ idToken }),
    });
    await createSession(auth);
  } catch (e) {
    return toFormState(e);
  }
  redirect("/");
}

export async function logoutAction(): Promise<void> {
  const session = await getSession();
  if (session) {
    // Endpoint public, idempotent — không quan tâm thành công hay thất bại, vẫn xoá cookie local
    await apiFetch("/api/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    }).catch(() => {});
  }
  await destroySession();
  redirect("/");
}

// Gộp xử lý lỗi dùng chung cho 3 action phía trên
function toFormState(e: unknown): AuthFormState {
  if (e instanceof ApiError) {
    return { error: e.message, fieldErrors: e.fieldErrors };
  }
  return { error: "Không thể kết nối máy chủ, vui lòng thử lại" };
}
