"use client";

import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { Play, Pause, Minus, Plus, X, AArrowDown, AArrowUp } from "lucide-react";
import SyntaxHighlighter from "@/components/converter/SyntaxHighlighter";
import { useAutoScroll, SPEED_STEP, MIN_SPEED, MAX_SPEED } from "@/hooks/useAutoScroll";
import { useWakeLock } from "@/hooks/useWakeLock";
import { toast } from "@/store/toastStore";

const FONT_SIZES = [16, 18, 20, 24, 28, 32, 40];
const FONT_STORAGE_KEY = "toneshift-performance-font";
const CONTROLS_HIDE_DELAY_MS = 2500;

interface PerformanceModeProps {
  title: string;
  text: string;
  toneLabel?: string;
  onClose: () => void;
}

function readStoredFontIndex(): number {
  try {
    const value = Number(window.localStorage.getItem(FONT_STORAGE_KEY));
    return Number.isInteger(value) && value >= 0 && value < FONT_SIZES.length ? value : 2;
  } catch {
    return 2;
  }
}

export default function PerformanceMode({ title, text, toneLabel, onClose }: PerformanceModeProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const { isPlaying, toggle, speed, setSpeed } = useAutoScroll(scrollRef);
  const [fontIndex, setFontIndex] = useState(readStoredFontIndex);
  const [controlsVisible, setControlsVisible] = useState(true);
  const hideTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useWakeLock(true, () =>
    toast.info("Trình duyệt không hỗ trợ giữ màn hình sáng. Hãy tắt chế độ tự khoá màn hình."),
  );

  const changeFont = (delta: number) => {
    setFontIndex((prev) => {
      const next = Math.min(FONT_SIZES.length - 1, Math.max(0, prev + delta));
      try {
        window.localStorage.setItem(FONT_STORAGE_KEY, String(next));
      } catch {
        // Ignore storage errors, font size still applies for this session
      }
      return next;
    });
  };

  // Controls fade out while playing and reappear on any pointer/keyboard activity
  const revealControls = () => {
    setControlsVisible(true);
    if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    hideTimerRef.current = setTimeout(() => setControlsVisible(false), CONTROLS_HIDE_DELAY_MS);
  };

  useEffect(() => () => {
    if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
  }, []);

  // Lock page scroll behind the overlay
  useEffect(() => {
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, []);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      switch (e.key) {
        case " ":
          e.preventDefault();
          toggle();
          break;
        case "ArrowUp":
          e.preventDefault();
          setSpeed((s) => s + SPEED_STEP);
          break;
        case "ArrowDown":
          e.preventDefault();
          setSpeed((s) => s - SPEED_STEP);
          break;
        case "+":
        case "=":
          changeFont(1);
          break;
        case "-":
          changeFont(-1);
          break;
        case "Escape":
          onClose();
          break;
        default:
          return;
      }
      revealControls();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [toggle, setSpeed, onClose]);

  const showControls = controlsVisible || !isPlaying;

  return (
    <motion.div
      role="dialog"
      aria-modal="true"
      aria-label={`Chế độ biểu diễn: ${title}`}
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[80] bg-black text-white"
      onPointerMove={revealControls}
    >
      <div
        ref={scrollRef}
        className="h-full overflow-y-auto px-6 md:px-16 pt-20 pb-[50vh]"
        onClick={() => {
          toggle();
          revealControls();
        }}
      >
        <div className="mx-auto max-w-3xl">
          <h2 className="text-2xl md:text-3xl font-bold mb-2">{title}</h2>
          {toneLabel && <p className="text-sm text-white/50 mb-10">{toneLabel}</p>}
          <div style={{ fontSize: FONT_SIZES[fontIndex] }} className="[&_*]:!text-[length:inherit] leading-loose">
            <SyntaxHighlighter text={text} />
          </div>
        </div>
      </div>

      <button
        onClick={onClose}
        aria-label="Thoát chế độ biểu diễn (Esc)"
        className={`absolute top-4 right-4 w-11 h-11 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center transition-opacity duration-300 ${showControls ? "opacity-100" : "opacity-0"}`}
      >
        <X className="w-5 h-5" />
      </button>

      <div
        className={`fixed bottom-6 left-1/2 -translate-x-1/2 flex items-center gap-1 sm:gap-2 bg-[#111] border border-white/10 rounded-full px-3 sm:px-4 py-2 shadow-2xl transition-opacity duration-300 ${showControls ? "opacity-100" : "opacity-0 pointer-events-none"}`}
      >
        <button
          onClick={() => changeFont(-1)}
          disabled={fontIndex === 0}
          aria-label="Giảm cỡ chữ (-)"
          className="w-10 h-10 rounded-full flex items-center justify-center text-white/60 hover:text-white disabled:opacity-30"
        >
          <AArrowDown className="w-5 h-5" />
        </button>
        <button
          onClick={() => changeFont(1)}
          disabled={fontIndex === FONT_SIZES.length - 1}
          aria-label="Tăng cỡ chữ (+)"
          className="w-10 h-10 rounded-full flex items-center justify-center text-white/60 hover:text-white disabled:opacity-30"
        >
          <AArrowUp className="w-5 h-5" />
        </button>

        <span className="w-px h-6 bg-white/10 mx-1" />

        <button
          onClick={() => setSpeed((s) => s - SPEED_STEP)}
          disabled={speed <= MIN_SPEED}
          aria-label="Giảm tốc độ (mũi tên xuống)"
          className="w-10 h-10 rounded-full flex items-center justify-center text-white/60 hover:text-white disabled:opacity-30"
        >
          <Minus className="w-4 h-4" />
        </button>
        <span className="w-12 text-center text-xs font-mono text-primary tabular-nums" aria-live="polite">
          {speed}x
        </span>
        <button
          onClick={() => setSpeed((s) => s + SPEED_STEP)}
          disabled={speed >= MAX_SPEED}
          aria-label="Tăng tốc độ (mũi tên lên)"
          className="w-10 h-10 rounded-full flex items-center justify-center text-white/60 hover:text-white disabled:opacity-30"
        >
          <Plus className="w-4 h-4" />
        </button>

        <button
          onClick={() => {
            toggle();
            revealControls();
          }}
          aria-label={isPlaying ? "Tạm dừng (Space)" : "Bắt đầu cuộn (Space)"}
          className="ml-1 w-12 h-12 rounded-full bg-primary text-primary-foreground flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
        >
          {isPlaying ? <Pause className="w-5 h-5" fill="currentColor" /> : <Play className="w-5 h-5 ml-0.5" fill="currentColor" />}
        </button>
      </div>

      <p
        className={`hidden md:block fixed bottom-24 left-1/2 -translate-x-1/2 text-xs text-white/30 pointer-events-none transition-opacity duration-300 ${showControls && !isPlaying ? "opacity-100" : "opacity-0"}`}
      >
        Space: chạy/dừng · ↑↓: tốc độ · +/−: cỡ chữ · Esc: thoát
      </p>
    </motion.div>
  );
}
