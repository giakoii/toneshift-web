// Server-only: không import file này vào Client Component
import type { ApiErrorBody, FieldErrorItem } from "@/types/auth";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly fieldErrors: FieldErrorItem[] = []
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const DEFAULT_BASE_URL = "http://localhost:8080";

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const baseUrl = process.env.API_BASE_URL || DEFAULT_BASE_URL;
  const url = `${baseUrl.replace(/\/+$/, "")}/${path.replace(/^\/+/, "")}`;

  const headers = new Headers(init?.headers);
  const isGetWithoutBody = (!init?.method || init.method.toUpperCase() === "GET") && !init?.body;
  if (!headers.has("Content-Type") && !isGetWithoutBody) {
    headers.set("Content-Type", "application/json");
  }

  let res: Response;
  try {
    res = await fetch(url, {
      ...init,
      headers,
      cache: "no-store",
    });
  } catch {
    throw new ApiError(0, "NETWORK_ERROR", "Không thể kết nối máy chủ, vui lòng thử lại");
  }

  if (res.status === 204) {
    return undefined as unknown as T;
  }

  let body: Partial<ApiErrorBody> | null = null;
  try {
    body = await res.json();
  } catch {
    // Không parse được JSON
  }

  if (!res.ok) {
    throw new ApiError(
      res.status,
      body?.code ?? "UNKNOWN_ERROR",
      body?.message ?? "Đã xảy ra lỗi không xác định",
      body?.errors ?? []
    );
  }

  return body as T;
}
