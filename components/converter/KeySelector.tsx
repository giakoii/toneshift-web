"use client";

import { CHROMATIC_SCALE, KEY_DISPLAY_NAMES } from "@/constants/musical-keys";
import { useConverterStore } from "@/store/converterStore";
import { ChevronDown } from "lucide-react";

interface KeySelectorProps {
  variant: "from" | "to";
}

export default function KeySelector({ variant }: KeySelectorProps) {
  const fromKey = useConverterStore((s) => s.fromKey);
  const toKey = useConverterStore((s) => s.toKey);
  const setFromKey = useConverterStore((s) => s.setFromKey);
  const setToKey = useConverterStore((s) => s.setToKey);

  const value = variant === "from" ? fromKey : toKey;
  const onChange = variant === "from" ? setFromKey : setToKey;
  const label = variant === "from" ? "Tone gốc" : "Tone đích";

  return (
    <div className="flex items-center gap-2">
      <span className="text-xs font-medium text-white/40 uppercase tracking-wider shrink-0">
        {label}
      </span>
      <div className="relative">
        <select
          id={`key-selector-${variant}`}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className={`
            appearance-none cursor-pointer
            pl-3 pr-8 py-1.5
            rounded-lg text-sm font-semibold
            bg-white/[0.07] border border-white/[0.1]
            text-white
            hover:bg-white/[0.12]
            focus:outline-none focus:ring-2 focus:ring-amber-500/40
            transition-all duration-200
          `}
        >
          {CHROMATIC_SCALE.map((key, i) => (
            <option key={key} value={key} className="bg-[oklch(0.17_0.015_45)] text-white">
              {KEY_DISPLAY_NAMES[i]}
            </option>
          ))}
        </select>
        <ChevronDown className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-white/40" />
      </div>
    </div>
  );
}
