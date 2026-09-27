"use client";
import { motion, useInView, useReducedMotion } from "framer-motion";
import { useRef, useState, useMemo } from "react";
import { transposeText, parseTokens } from "@/lib/transpose";
import { CHROMATIC_SCALE, KEY_DISPLAY_NAMES } from "@/constants/musical-keys";
import { ChevronDown, ArrowRightLeft } from "lucide-react";
import { AnimatePresence } from "framer-motion";
import type { Token } from "@/lib/transpose";
import { ScrollReveal } from "./ScrollReveal";

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
              ? "text-primary font-bold"
              : token.type === "separator"
                ? "text-foreground/20"
                : "text-foreground/40"
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
  const displayValue = KEY_DISPLAY_NAMES[CHROMATIC_SCALE.indexOf(value)];

  return (
    <div className="flex items-center gap-2">
      <span className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground shrink-0">
        {label}
      </span>
      <div className="relative overflow-hidden rounded-md border border-border bg-background">
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="appearance-none cursor-pointer relative z-10 w-[60px] bg-transparent pl-3 pr-6 py-1.5 text-xs font-semibold text-transparent hover:bg-card focus:outline-none transition-colors"
        >
          {CHROMATIC_SCALE.map((key, i) => (
            <option
              key={key}
              value={key}
              className="bg-background text-foreground"
            >
              {KEY_DISPLAY_NAMES[i]}
            </option>
          ))}
        </select>

        <div className="pointer-events-none absolute inset-y-0 left-3 flex items-center justify-start z-0">
          <AnimatePresence mode="popLayout">
            <motion.span
              key={value}
              initial={{ y: -15, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: 15, opacity: 0 }}
              transition={{ duration: 0.25, ease: "easeInOut" }}
              className="text-xs font-semibold text-foreground block"
            >
              {displayValue}
            </motion.span>
          </AnimatePresence>
        </div>

        <ChevronDown className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 w-3 h-3 text-muted-foreground z-0" />
      </div>
    </div>
  );
}

export function CinematicConverter() {
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
      id="converter"
      className="relative flex justify-center px-6 py-24 md:py-32 bg-background overflow-hidden"
    >
      <div className="mx-auto max-w-4xl w-full">
        <ScrollReveal>
          <div className="mb-12 text-center">
            <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em] leading-tight mb-4">
              Thử ngay tại đây.
            </h2>
            <p className="mx-auto mt-3 max-w-md text-lg text-muted-foreground font-light">
              Đổi tone gốc và xem kết quả tính toán ngay lập tức.
            </p>
          </div>
        </ScrollReveal>

        <ScrollReveal delay={0.2}>
          <div className="mb-6 flex items-center justify-center gap-4">
            <MiniKeySelect value={fromKey} onChange={setFromKey} label="Từ" />
            <button
              onClick={handleSwap}
              aria-label="Đổi chiều tone"
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-card border border-border text-muted-foreground hover:text-foreground hover:bg-card/80 hover:border-primary/50 transition-all active:scale-95"
            >
              <ArrowRightLeft className="w-3.5 h-3.5" />
            </button>
            <MiniKeySelect value={toKey} onChange={setToKey} label="Sang" />
          </div>

          <div className="rounded-3xl border-[1px] border-border bg-card overflow-hidden shadow-2xl relative">
            <div className="grid grid-cols-1 md:grid-cols-2">
              <div className="p-6 md:p-8 border-b md:border-b-0 md:border-r border-border bg-background/50">
                <div className="mb-4 flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-widest text-muted-foreground">
                    Cảm âm gốc
                  </span>
                  <span className="rounded-md bg-card border border-border px-3 py-1 text-[10px] font-bold text-foreground">
                    Tone: {fromKey}
                  </span>
                </div>
                <pre className="font-mono text-sm leading-relaxed text-foreground/70 whitespace-pre-wrap">
                  {DEMO_INPUT}
                </pre>
              </div>

              <div className="p-6 md:p-8 bg-gradient-to-br from-card to-background">
                <div className="mb-4 flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-widest text-primary">
                    Kết quả
                  </span>
                  <span className="rounded-md bg-primary/10 border border-primary/20 px-3 py-1 text-[10px] font-bold text-primary">
                    Tone: {toKey}
                  </span>
                </div>
                <div className="font-mono text-sm leading-relaxed whitespace-pre-wrap">
                  <TokenRenderer tokens={outputTokens} />
                </div>
              </div>
            </div>
          </div>
        </ScrollReveal>
      </div>
    </section>
  );
}
