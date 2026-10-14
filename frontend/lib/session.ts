import { cookies } from "next/headers";
import { randomBytes, createCipheriv, createDecipheriv } from "node:crypto";
import type { AuthResponse, UserResponse } from "@/types/auth";

const ALGORITHM = "aes-256-gcm";
const IV_LENGTH = 12; // khuyến nghị cho GCM

export interface SessionPayload {
  user: UserResponse;
  accessToken: string;
  accessTokenExpiresAt: number; // epoch ms
  refreshToken: string;
}

function getSessionKey(): Buffer {
  const secret = process.env.SESSION_SECRET;
  if (!secret) {
    throw new Error("Thiếu biến môi trường SESSION_SECRET");
  }
  const key = Buffer.from(secret, "base64");
  if (key.length !== 32) {
    throw new Error("SESSION_SECRET phải là base64 của đúng 32 byte");
  }
  return key;
}

function encryptSession(payload: SessionPayload): string {
  const key = getSessionKey();
  const iv = randomBytes(IV_LENGTH);
  const cipher = createCipheriv(ALGORITHM, key, iv);
  const plaintext = Buffer.from(JSON.stringify(payload), "utf8");
  const encrypted = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  const authTag = cipher.getAuthTag();
  // base64url từng phần, nối bằng dấu chấm — giống cấu trúc JWT cho dễ debug bằng mắt
  return [iv, authTag, encrypted].map((b) => b.toString("base64url")).join(".");
}

function decryptSession(token: string): SessionPayload | null {
  try {
    const [ivB64, tagB64, dataB64] = token.split(".");
    if (!ivB64 || !tagB64 || !dataB64) return null;
    const key = getSessionKey();
    const decipher = createDecipheriv(ALGORITHM, key, Buffer.from(ivB64, "base64url"));
    decipher.setAuthTag(Buffer.from(tagB64, "base64url"));
    const decrypted = Buffer.concat([
      decipher.update(Buffer.from(dataB64, "base64url")),
      decipher.final(),
    ]);
    return JSON.parse(decrypted.toString("utf8")) as SessionPayload;
  } catch {
    // Token hỏng/sai key/hết hạn định dạng → coi như chưa đăng nhập, không throw
    return null;
  }
}

const COOKIE_NAME = "ts_session";
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30; // 30 ngày

export async function createSession(auth: AuthResponse): Promise<void> {
  const payload: SessionPayload = {
    user: auth.user,
    accessToken: auth.accessToken,
    accessTokenExpiresAt: Date.now() + auth.expiresIn * 1000,
    refreshToken: auth.refreshToken,
  };
  const token = encryptSession(payload);
  const store = await cookies();
  store.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: COOKIE_MAX_AGE_SECONDS,
  });
}

export async function getSession(): Promise<SessionPayload | null> {
  const store = await cookies();
  const cookie = store.get(COOKIE_NAME);
  if (!cookie) return null;
  return decryptSession(cookie.value);
}

export async function destroySession(): Promise<void> {
  const store = await cookies();
  store.delete(COOKIE_NAME);
}
