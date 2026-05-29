"use client";

import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { Sun, Moon, Monitor } from "lucide-react";

type Mode = "light" | "dark" | "system";

const MODES: { value: Mode; icon: React.ReactNode; label: string }[] = [
  { value: "light", icon: <Sun size={14} />, label: "Sáng" },
  { value: "dark", icon: <Moon size={14} />, label: "Tối" },
  { value: "system", icon: <Monitor size={14} />, label: "Hệ thống" },
];

interface ThemeToggleProps {
  variant?: "icon" | "menu";
}

export function ThemeToggle({ variant = "icon" }: ThemeToggleProps) {
  const { theme, setTheme, resolvedTheme } = useTheme();
  const [mounted, setMounted] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => setMounted(true), []);
  if (!mounted) {
    return (
      <div className="w-9 h-9 rounded-lg bg-white/[0.05] border border-white/[0.08] animate-pulse" />
    );
  }

  if (variant === "icon") {
    const isDark = resolvedTheme === "dark";
    return (
      <button
        id="theme-toggle-btn"
        onClick={() => setTheme(isDark ? "light" : "dark")}
        aria-label={isDark ? "Chuyển sang giao diện sáng" : "Chuyển sang giao diện tối"}
        title={isDark ? "Giao diện sáng" : "Giao diện tối"}
        className="
          relative w-9 h-9 flex items-center justify-center rounded-lg
          bg-white/[0.05] border border-white/[0.08]
          text-zinc-400 hover:text-[#FAFAFA]
          hover:bg-white/[0.08] hover:border-white/[0.14]
          dark:hover:text-[#FAFAFA]
          transition-all duration-200 cursor-pointer
          group overflow-hidden
        "
      >
        <Sun
          size={16}
          className={`
            absolute transition-all duration-300
            ${isDark
              ? "opacity-0 rotate-90 scale-50"
              : "opacity-100 rotate-0 scale-100 text-amber-400"}
          `}
        />
        <Moon
          size={16}
          className={`
            absolute transition-all duration-300
            ${isDark
              ? "opacity-100 rotate-0 scale-100 text-blue-300"
              : "opacity-0 -rotate-90 scale-50"}
          `}
        />
      </button>
    );
  }

  const current = MODES.find((m) => m.value === theme) ?? MODES[1];

  return (
    <div className="relative">
      <button
        id="theme-menu-btn"
        onClick={() => setMenuOpen((v) => !v)}
        aria-label="Chọn giao diện"
        className="
          flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm
          bg-white/[0.05] border border-white/[0.08]
          text-zinc-400 hover:text-[#FAFAFA]
          hover:bg-white/[0.08] transition-all duration-200 cursor-pointer
        "
      >
        {current.icon}
        <span>{current.label}</span>
      </button>

      {menuOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-10"
            onClick={() => setMenuOpen(false)}
          />
          {/* Dropdown */}
          <div className="
            absolute right-0 top-full mt-2 z-20
            bg-[#18181B] border border-white/[0.08]
            rounded-xl p-1.5 shadow-2xl shadow-black/50
            min-w-[150px]
          ">
            {MODES.map((mode) => (
              <button
                key={mode.value}
                id={`theme-option-${mode.value}`}
                onClick={() => {
                  setTheme(mode.value);
                  setMenuOpen(false);
                }}
                className={`
                  w-full flex items-center gap-2.5 px-3 py-2 text-sm rounded-lg
                  transition-all duration-150 cursor-pointer text-left
                  ${theme === mode.value
                    ? "bg-[#2563EB]/15 text-[#60A5FA]"
                    : "text-zinc-400 hover:text-[#FAFAFA] hover:bg-white/[0.06]"}
                `}
              >
                {mode.icon}
                {mode.label}
                {theme === mode.value && (
                  <span className="ml-auto w-1.5 h-1.5 rounded-full bg-[#60A5FA]" />
                )}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
