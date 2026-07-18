"use client";

import { motion, useInView, useReducedMotion } from "framer-motion";
import { useRef, useState, useMemo } from "react";
import { transposeText, parseTokens } from "@/lib/transpose";
import { CHROMATIC_SCALE, KEY_DISPLAY_NAMES } from "@/constants/musical-keys";
import { ChevronDown, ArrowRightLeft } from "lucide-react";
import type { Token } from "@/lib/transpose";

const DEMO_INPUT = `Am  F  C  G
Dm  Am  Bb  F
Am  F  C  G`;

function TokenRenderer({ tokens }: { tokens: Token[] }) {
  return (
    <>
      {tokens.map((token, i) => (
        <span
          key={i}
          className={
            token.type === "note"
              ? "text-amber-400 font-bold"
              : token.type === "separator"
                ? "text-white/20"
                : "text-white/40"
          }
        >
          {token.value}
        </span>
      ))}
    </>
  );
}

function MiniKeySelect({
  value,
  onChange,
  label,
}: {
  value: string;
  onChange: (v: string) => void;
  label: string;
}) {
  return (
    <div className="flex items-center gap-1.5">
      <span className="text-[10px] font-medium uppercase tracking-wider text-white/25">
        {label}
      </span>
      <div className="relative">
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="appearance-none cursor-pointer rounded-md bg-white/[0.06] pl-2 pr-6 py-0.5 text-[11px] font-semibold text-white/60 border border-white/[0.08] hover:bg-white/[0.1] focus:outline-none transition-colors"
        >
          {CHROMATIC_SCALE.map((key, i) => (
            <option
              key={key}
              value={key}
              className="bg-[oklch(0.17_0.015_45)] text-white"
            >
              {KEY_DISPLAY_NAMES[i]}
            </option>
          ))}
        </select>
        <ChevronDown className="pointer-events-none absolute right-1 top-1/2 -translate-y-1/2 w-3 h-3 text-white/30" />
      </div>
    </div>
  );
}

export default function ConverterShowcase() {
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { once: true, margin: "-80px" });
  const reduce = useReducedMotion();

  const [fromKey, setFromKey] = useState("C");
  const [toKey, setToKey] = useState("G");

  const output = useMemo(
    () => transposeText(DEMO_INPUT, fromKey, toKey),
    [fromKey, toKey],
  );
  const outputTokens = useMemo(() => parseTokens(output), [output]);

  const handleSwap = () => {
    const prev = fromKey;
    setFromKey(toKey);
    setToKey(prev);
  };

  return (
    <section
      ref={ref}
      className="relative flex justify-center px-6 py-28 sm:py-36 lg:py-44"
    >
      <motion.div
        initial={reduce ? false : { opacity: 0, y: 48 }}
        animate={inView ? { opacity: 1, y: 0 } : {}}
        transition={{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }}
        className="w-full max-w-3xl"
      >
        {/* Section header - no eyebrow */}
        <div className="mb-10 text-center">
          <h2 className="text-2xl font-bold text-white sm:text-3xl">
            Thử ngay trên trang này
          </h2>
          <p className="mx-auto mt-3 max-w-md text-sm leading-relaxed text-white/40">
            Thay đổi tone bên dưới để xem kết quả chuyển đổi trực tiếp.
          </p>
        </div>

        {/* Key selectors */}
        <div className="mb-4 flex items-center justify-center gap-3">
          <MiniKeySelect value={fromKey} onChange={setFromKey} label="Từ" />
          <button
            onClick={handleSwap}
            aria-label="Đổi chiều tone"
            className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/[0.04] border border-white/[0.08] text-white/30 hover:text-white/60 hover:bg-white/[0.08] transition-all active:scale-90"
          >
            <ArrowRightLeft className="w-3 h-3" />
          </button>
          <MiniKeySelect value={toKey} onChange={setToKey} label="Sang" />
        </div>

        {/* Live converter panels */}
        <div className="rounded-2xl border border-white/[0.08] bg-white/[0.02] overflow-hidden">
          <div className="grid grid-cols-1 sm:grid-cols-2">
            {/* Input panel */}
            <div className="p-5">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-[10px] font-medium uppercase tracking-wider text-white/25">
                  Cảm âm gốc
                </span>
                <span className="rounded-md bg-white/[0.06] px-2 py-0.5 text-[10px] font-semibold text-white/40">
                  {fromKey}
                </span>
              </div>
              <pre className="font-mono text-sm leading-relaxed text-white/60 whitespace-pre-wrap">
                {DEMO_INPUT}
              </pre>
            </div>

            {/* Output panel */}
            <div className="border-t border-white/[0.06] p-5 sm:border-t-0 sm:border-l">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-[10px] font-medium uppercase tracking-wider text-amber-400/50">
                  Kết quả
                </span>
                <span className="rounded-md bg-amber-500/10 px-2 py-0.5 text-[10px] font-semibold text-amber-400/70">
                  {toKey}
                </span>
              </div>
              <div className="font-mono text-sm leading-relaxed whitespace-pre-wrap">
                <TokenRenderer tokens={outputTokens} />
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </section>
  );
}
