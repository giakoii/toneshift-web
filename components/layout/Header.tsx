"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useScrolled } from "@/hooks/useScrolled";
import { NAV_LINKS } from "@/constants/navigation";
import { Menu, X } from "lucide-react";
import logo from "@/assets/logo.png";
import Image from "next/image";

export default function Header() {
  const scrolled = useScrolled(30);
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header
      className={`
        fixed top-0 left-0 right-0 z-50
        flex justify-center
        px-4 pt-4
        transition-all duration-500 ease-out
        pointer-events-none
      `}
    >
      {/* Floating glass pill nav */}
      <nav
        className={`
          pointer-events-auto
          flex items-center justify-between
          w-full max-w-5xl
          px-4 py-2.5
          rounded-2xl
          border border-white/[0.08]
          transition-all duration-500 ease-out
          ${
            scrolled
              ? "bg-[oklch(0.10_0_0/0.85)] backdrop-blur-xl shadow-[0_8px_40px_oklch(0_0_0/0.5)]"
              : "bg-[oklch(0.10_0_0/0.4)] backdrop-blur-md shadow-[0_2px_20px_oklch(0_0_0/0.2)]"
          }
        `}
      >
        {/* Logo */}
        <Link
          href="/"
          className="flex items-center gap-2.5 group select-none"
          aria-label="ToneShift – về trang chủ"
        >
          <Image src={logo} alt="ToneShift Logo" width={32} height={32} className="rounded-full" />
          <span className="font-semibold text-[15px] tracking-tight text-white">
            Tone<span className="text-blue-400">Shift</span>
          </span>
        </Link>

        {/* Desktop nav links */}
        <ul className="hidden md:flex items-center gap-1" role="list">
          {NAV_LINKS.map((link) => (
            <li key={link.href}>
              <Link
                href={link.href}
                className={`
                  relative px-3.5 py-1.5 text-sm font-medium
                  text-white/60 rounded-lg
                  hover:text-white
                  transition-colors duration-200
                  hover:bg-white/[0.07]
                `}
              >
                {link.label}
              </Link>
            </li>
          ))}
        </ul>

        {/* CTA + mobile toggle */}
        <div className="flex items-center gap-2">
          {/* Desktop CTA */}
          <Link
            href="#get-started"
            id="header-cta"
            className={`
              hidden md:inline-flex items-center gap-1.5
              px-4 py-2 rounded-xl text-sm font-semibold
              bg-gradient-to-r from-blue-600 to-blue-500
              text-white
              shadow-[0_0_20px_oklch(0.49_0.24_264/0.35)]
              hover:shadow-[0_0_28px_oklch(0.49_0.24_264/0.6)]
              hover:brightness-110
              transition-all duration-300
              active:scale-95
            `}
          >
            Bắt đầu miễn phí
          </Link>

          {/* Mobile hamburger */}
          <button
            id="mobile-menu-toggle"
            onClick={() => setMobileOpen((v) => !v)}
            aria-label={mobileOpen ? "Đóng menu" : "Mở menu"}
            aria-expanded={mobileOpen}
            className={`
              md:hidden flex items-center justify-center
              w-9 h-9 rounded-xl
              text-white/70 hover:text-white
              hover:bg-white/[0.08]
              transition-all duration-200
            `}
          >
            {mobileOpen ? (
              <X className="w-5 h-5" />
            ) : (
              <Menu className="w-5 h-5" />
            )}
          </button>
        </div>
      </nav>

      {/* Mobile dropdown */}
      <div
        aria-hidden={!mobileOpen}
        className={`
          pointer-events-auto
          absolute top-[72px] left-4 right-4
          rounded-2xl
          border border-white/[0.08]
          bg-[oklch(0.10_0_0/0.92)] backdrop-blur-xl
          shadow-[0_16px_48px_oklch(0_0_0/0.6)]
          overflow-hidden
          transition-all duration-300 ease-out
          ${mobileOpen ? "opacity-100 translate-y-0" : "opacity-0 -translate-y-2 pointer-events-none"}
        `}
      >
        <ul className="flex flex-col p-2" role="list">
          {NAV_LINKS.map((link) => (
            <li key={link.href}>
              <Link
                href={link.href}
                onClick={() => setMobileOpen(false)}
                className={`
                  flex items-center px-4 py-3 rounded-xl
                  text-sm font-medium text-white/70
                  hover:text-white hover:bg-white/[0.07]
                  transition-all duration-200
                `}
              >
                {link.label}
              </Link>
            </li>
          ))}
        </ul>

        <div className="px-2 pb-2">
          <Link
            href="#get-started"
            onClick={() => setMobileOpen(false)}
            id="mobile-cta"
            className={`
              flex items-center justify-center
              w-full py-3 rounded-xl
              text-sm font-semibold
              bg-gradient-to-r from-blue-600 to-blue-500
              text-white
              transition-all duration-200
              active:scale-95
            `}
          >
            Bắt đầu miễn phí
          </Link>
        </div>
      </div>
    </header>
  );
}