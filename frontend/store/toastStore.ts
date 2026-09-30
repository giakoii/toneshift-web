import { create } from "zustand";

export type ToastType = "success" | "error" | "info";

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

interface ToastStore {
  toasts: Toast[];
  show: (type: ToastType, message: string) => void;
  dismiss: (id: number) => void;
}

const TOAST_DURATION_MS = 3500;
let nextId = 1;

export const useToastStore = create<ToastStore>((set, get) => ({
  toasts: [],

  show: (type, message) => {
    const id = nextId++;
    // Keep at most 3 toasts on screen, newest last
    set((state) => ({ toasts: [...state.toasts.slice(-2), { id, type, message }] }));
    setTimeout(() => get().dismiss(id), TOAST_DURATION_MS);
  },

  dismiss: (id) =>
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) })),
}));

// Shorthand usable outside React components (e.g. inside mock API helpers)
export const toast = {
  success: (message: string) => useToastStore.getState().show("success", message),
  error: (message: string) => useToastStore.getState().show("error", message),
  info: (message: string) => useToastStore.getState().show("info", message),
};
