"use client";

import { useState, useEffect, useCallback } from "react";
import type { HistoryItem, ToneKey } from "@/types";
import { LOCAL_STORAGE_KEYS, HISTORY_MAX_ITEMS } from "@/constants";

export function useHistory() {
  const [history, setHistory] = useState<HistoryItem[]>([]);

  // Load from localStorage on mount
  useEffect(() => {
    try {
      const raw = localStorage.getItem(LOCAL_STORAGE_KEYS.HISTORY);
      if (raw) setHistory(JSON.parse(raw) as HistoryItem[]);
    } catch {
      // ignore parse errors
    }
  }, []);

  const persist = useCallback((items: HistoryItem[]) => {
    setHistory(items);
    localStorage.setItem(LOCAL_STORAGE_KEYS.HISTORY, JSON.stringify(items));
  }, []);

  const addItem = useCallback(
    (item: Omit<HistoryItem, "id" | "createdAt">) => {
      const newItem: HistoryItem = {
        ...item,
        id: crypto.randomUUID(),
        createdAt: new Date().toISOString(),
      };
      persist([newItem, ...history].slice(0, HISTORY_MAX_ITEMS));
    },
    [history, persist]
  );

  const removeItem = useCallback(
    (id: string) => persist(history.filter((h) => h.id !== id)),
    [history, persist]
  );

  const clearAll = useCallback(() => {
    persist([]);
  }, [persist]);

  return { history, addItem, removeItem, clearAll };
}
