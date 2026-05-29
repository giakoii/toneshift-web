// ─── History ─────────────────────────────────────────────────────────────────

import type { ToneKey } from "./music";

export interface HistoryItem {
  id: string;
  title: string;
  originalTone: ToneKey;
  targetTone: ToneKey;
  inputPreview: string;   // first ~60 chars
  outputPreview: string;
  createdAt: string;      // ISO string
}

// ─── User ────────────────────────────────────────────────────────────────────

export interface User {
  id: string;
  displayName: string;
  email: string;
  avatarUrl?: string;
  createdAt: string;
}

// ─── API Response Shape ───────────────────────────────────────────────────────
// Used as placeholder while API is not connected yet.

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
  hasNext: boolean;
}

// ─── Filter / Search ─────────────────────────────────────────────────────────

export interface SongFilter {
  genre?: string;
  tone?: ToneKey;
  query?: string;
  page?: number;
  pageSize?: number;
  sortBy?: "latest" | "popular" | "alphabetical";
}
