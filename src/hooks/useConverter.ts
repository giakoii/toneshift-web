"use client";

import { useState, useCallback } from "react";
import type { ConvertInput, ConvertResult, ToneKey } from "@/types";
import { convert } from "@/lib/transpose/transposer";
import { detectTone } from "@/lib/transpose/detector";

interface UseConverterReturn {
  inputText: string;
  setInputText: (text: string) => void;
  fromTone: ToneKey;
  setFromTone: (tone: ToneKey) => void;
  toTone: ToneKey;
  setToTone: (tone: ToneKey) => void;
  result: ConvertResult | null;
  detectedTone: ToneKey | null;
  convertNow: () => void;
  reset: () => void;
}

export function useConverter(defaultFrom: ToneKey = "D", defaultTo: ToneKey = "C"): UseConverterReturn {
  const [inputText, setInputTextState] = useState("");
  const [fromTone, setFromTone] = useState<ToneKey>(defaultFrom);
  const [toTone, setToTone] = useState<ToneKey>(defaultTo);
  const [result, setResult] = useState<ConvertResult | null>(null);
  const [detectedTone, setDetectedTone] = useState<ToneKey | null>(null);

  /** Update input and run auto-detect */
  const setInputText = useCallback((text: string) => {
    setInputTextState(text);
    if (text.trim()) {
      const detected = detectTone(text);
      setDetectedTone(detected);
    } else {
      setDetectedTone(null);
    }
  }, []);

  /** Run conversion */
  const convertNow = useCallback(() => {
    if (!inputText.trim()) return;
    const input: ConvertInput = { text: inputText, fromTone, toTone };
    setResult(convert(input));
  }, [inputText, fromTone, toTone]);

  const reset = useCallback(() => {
    setInputTextState("");
    setResult(null);
    setDetectedTone(null);
  }, []);

  return {
    inputText,
    setInputText,
    fromTone,
    setFromTone,
    toTone,
    setToTone,
    result,
    detectedTone,
    convertNow,
    reset,
  };
}
