"use client";

import { CHROMATIC_SCALE, KEY_DISPLAY_NAMES } from "@/constants/musical-keys";
import { useConverterStore } from "@/store/converterStore";
import { ChevronDown } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

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
  const displayValue = KEY_DISPLAY_NAMES[CHROMATIC_SCALE.indexOf(value)];

  return (
    <div className="flex items-center gap-2">
      <span className="text-xs font-medium text-white/40 uppercase tracking-wider shrink-0">
        {label}
      </span>
      <div className="relative overflow-hidden rounded-lg">
        <select
          id={`key-selector-${variant}`}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className={`
            appearance-none cursor-pointer relative z-10
            pl-3 pr-8 py-1.5
            w-[72px] /* Fixed width to prevent jumping */
            rounded-lg text-sm font-semibold
            bg-white/[0.07] border border-white/[0.1]
            text-transparent /* Hide native text to show animated text underneath */
            hover:bg-white/[0.12]
            focus:outline-none focus:ring-2 focus:ring-amber-500/40
            transition-colors duration-200
          `}
        >
          {CHROMATIC_SCALE.map((key, i) => (
            <option key={key} value={key} className="bg-[oklch(0.17_0.015_45)] text-white">
              {KEY_DISPLAY_NAMES[i]}
            </option>
          ))}
        </select>
        
        {/* Animated Label */}
        <div className="pointer-events-none absolute inset-y-0 left-3 flex items-center justify-start z-0">
          <AnimatePresence mode="popLayout">
            <motion.span
              key={value}
              initial={{ y: -20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 20, opacity: 0 }}
              transition={{ duration: 0.25, ease: "easeInOut" }}
              className="text-sm font-semibold text-white block"
            >
              {displayValue}
            </motion.span>
          </AnimatePresence>
        </div>

        <ChevronDown className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-white/40 z-0" />
      </div>
    </div>
  );
}
