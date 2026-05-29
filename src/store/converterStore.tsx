"use client";

/**
 * Converter Store – lightweight React Context-based state.
 * Will be replaced with Zustand when the app grows more complex.
 *
 * This keeps the converter state global so that:
 * - Header search can pre-fill the converter
 * - History saves automatically after conversion
 */

import React, { createContext, useContext, useReducer } from "react";
import type { ToneKey, ConvertResult } from "@/types";

// ─── State ───────────────────────────────────────────────────────────────────

interface ConverterState {
  inputText: string;
  fromTone: ToneKey;
  toTone: ToneKey;
  result: ConvertResult | null;
  detectedTone: ToneKey | null;
  isConverting: boolean;
}

const initialState: ConverterState = {
  inputText: "",
  fromTone: "D",
  toTone: "C",
  result: null,
  detectedTone: null,
  isConverting: false,
};

// ─── Actions ─────────────────────────────────────────────────────────────────

type Action =
  | { type: "SET_INPUT"; payload: string }
  | { type: "SET_FROM_TONE"; payload: ToneKey }
  | { type: "SET_TO_TONE"; payload: ToneKey }
  | { type: "SET_RESULT"; payload: ConvertResult }
  | { type: "SET_DETECTED_TONE"; payload: ToneKey | null }
  | { type: "SET_CONVERTING"; payload: boolean }
  | { type: "RESET" };

function reducer(state: ConverterState, action: Action): ConverterState {
  switch (action.type) {
    case "SET_INPUT":       return { ...state, inputText: action.payload };
    case "SET_FROM_TONE":   return { ...state, fromTone: action.payload };
    case "SET_TO_TONE":     return { ...state, toTone: action.payload };
    case "SET_RESULT":      return { ...state, result: action.payload, isConverting: false };
    case "SET_DETECTED_TONE": return { ...state, detectedTone: action.payload };
    case "SET_CONVERTING":  return { ...state, isConverting: action.payload };
    case "RESET":           return initialState;
    default:                return state;
  }
}

// ─── Context ─────────────────────────────────────────────────────────────────

interface ConverterContextValue {
  state: ConverterState;
  dispatch: React.Dispatch<Action>;
}

const ConverterContext = createContext<ConverterContextValue | null>(null);

export function ConverterProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  return (
    <ConverterContext.Provider value={{ state, dispatch }}>
      {children}
    </ConverterContext.Provider>
  );
}

export function useConverterStore() {
  const ctx = useContext(ConverterContext);
  if (!ctx) throw new Error("useConverterStore must be used within ConverterProvider");
  return ctx;
}
