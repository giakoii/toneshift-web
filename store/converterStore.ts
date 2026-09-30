import { create } from "zustand";
import { transposeText } from "@/lib/transpose";

export const MAX_INPUT_LENGTH = 50_000;

interface ConverterStore {
  inputText: string;
  outputText: string;
  fromKey: string;
  toKey: string;
  setInputText: (text: string) => void;
  setFromKey: (key: string) => void;
  setToKey: (key: string) => void;
  clearAll: () => void;
}

// Recompute output whenever input or keys change
function computeOutput(input: string, from: string, to: string): string {
  if (!input.trim() || input.length > MAX_INPUT_LENGTH) return "";
  return transposeText(input, from, to);
}

export const useConverterStore = create<ConverterStore>((set) => ({
  inputText: "",
  outputText: "",
  fromKey: "C",
  toKey: "G",

  setInputText: (text) =>
    set((state) => ({
      inputText: text,
      outputText: computeOutput(text, state.fromKey, state.toKey),
    })),

  setFromKey: (key) =>
    set((state) => ({
      fromKey: key,
      outputText: computeOutput(state.inputText, key, state.toKey),
    })),

  setToKey: (key) =>
    set((state) => ({
      toKey: key,
      outputText: computeOutput(state.inputText, state.fromKey, key),
    })),

  clearAll: () =>
    set({ inputText: "", outputText: "", fromKey: "C", toKey: "G" }),
}));
