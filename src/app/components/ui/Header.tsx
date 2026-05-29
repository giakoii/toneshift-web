"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import Container from "@/app/components/Container";
import { Music2, Search, Menu, X, ChevronDown, Zap } from "lucide-react";
import { ThemeToggle } from "@/app/components/ui/ThemeToggle";

const navLinks = [
  { label: "Chuyển Tone", href: "/converter" },
  { label: "Thư Viện", href: "/library" },
  {
    label: "Công Cụ",
    href: "/tools",
    children: [
      { label: "Phát Hiện Tone", href: "/tools/detect" },
      { label: "So Sánh Tone", href: "/tools/compare" },
      { label: "Luyện Tập", href: "/tools/practice" },
    ],
  },
  { label: "Cộng Đồng", href: "/community" },
  { label: "Hướng Dẫn", href: "/docs" },
];

const Header = () => {
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 16);
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Close mobile menu on resize to desktop
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 1024) setMobileOpen(false);
    };
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  return (
    <>
      <header
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          scrolled
            ? "bg-white/90 dark:bg-[#09090B]/90 backdrop-blur-xl border-b border-black/[0.06] dark:border-white/[0.06] shadow-lg shadow-black/10 dark:shadow-black/20"
            : "bg-transparent"
        }`}
      >
        <Container>
          <div className="flex items-center justify-between h-16 lg:h-18">
            {/* ── Logo ── */}
            <Link
              href="/"
              className="flex items-center gap-2.5 group select-none"
            >
              <div className="relative w-8 h-8 rounded-lg bg-gradient-to-br from-[#2563EB] to-[#60A5FA] flex items-center justify-center shadow-lg shadow-blue-500/30 group-hover:shadow-blue-500/50 transition-all duration-300 group-hover:scale-105">
                <Music2 size={16} className="text-white" strokeWidth={2.5} />
                <div className="absolute inset-0 rounded-lg bg-gradient-to-br from-white/20 to-transparent" />
              </div>
              <div className="flex flex-col leading-none">
                <span className="text-[#FAFAFA] font-bold text-lg tracking-tight">
                  ToneShift
                </span>
                <span className="text-[#60A5FA] text-[10px] font-medium tracking-widest uppercase">
                  Cảm Âm Pro
                </span>
              </div>
            </Link>

            {/* ── Desktop Nav ── */}
            <nav className="hidden lg:flex items-center gap-1">
              {navLinks.map((link) =>
                link.children ? (
                  <div
                    key={link.label}
                    className="relative"
                    onMouseEnter={() => setOpenDropdown(link.label)}
                    onMouseLeave={() => setOpenDropdown(null)}
                  >
                    <button className="flex items-center gap-1 px-3.5 py-2 text-sm text-zinc-400 hover:text-[#FAFAFA] rounded-lg hover:bg-white/[0.06] transition-all duration-200 font-medium cursor-pointer">
                      {link.label}
                      <ChevronDown
                        size={14}
                        className={`transition-transform duration-200 ${
                          openDropdown === link.label ? "rotate-180" : ""
                        }`}
                      />
                    </button>

                    {/* Dropdown */}
                    <div
                      className={`absolute top-full left-1/2 -translate-x-1/2 pt-2 transition-all duration-200 ${
                        openDropdown === link.label
                          ? "opacity-100 translate-y-0 pointer-events-auto"
                          : "opacity-0 -translate-y-1 pointer-events-none"
                      }`}
                    >
                      <div className="bg-white dark:bg-[#18181B] border border-black/[0.08] dark:border-white/[0.08] rounded-xl p-1.5 shadow-2xl shadow-black/20 dark:shadow-black/50 min-w-[180px]">
                        {link.children.map((child) => (
                          <Link
                            key={child.label}
                            href={child.href}
                            className="flex items-center gap-2 px-3 py-2 text-sm text-zinc-400 hover:text-[#FAFAFA] hover:bg-white/[0.06] rounded-lg transition-all duration-150"
                          >
                            {child.label}
                          </Link>
                        ))}
                      </div>
                    </div>
                  </div>
                ) : (
                  <Link
                    key={link.label}
                    href={link.href}
                    className="px-3.5 py-2 text-sm text-zinc-400 hover:text-[#FAFAFA] rounded-lg hover:bg-white/[0.06] transition-all duration-200 font-medium"
                  >
                    {link.label}
                  </Link>
                )
              )}
            </nav>

            {/* ── Right Actions ── */}
            <div className="flex items-center gap-2">
              {/* Search */}
              <button
                id="header-search-btn"
                onClick={() => setSearchOpen(true)}
                aria-label="Tìm kiếm"
                className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/[0.05] border border-white/[0.08] text-zinc-500 hover:text-zinc-300 hover:bg-white/[0.08] transition-all duration-200 text-sm cursor-pointer"
              >
                <Search size={14} />
                <span className="hidden md:inline text-xs">Tìm kiếm...</span>
                <kbd className="hidden md:inline text-[10px] bg-white/[0.07] px-1.5 py-0.5 rounded font-mono">
                  ⌘K
                </kbd>
              </button>

              {/* Theme Toggle */}
              <ThemeToggle variant="icon" />

              {/* Login */}
              <Link
                href="/login"
                id="header-login-btn"
                className="hidden sm:block px-3.5 py-1.5 text-sm font-medium text-zinc-400 hover:text-[#FAFAFA] dark:hover:text-[#FAFAFA] transition-colors duration-200"
              >
                Đăng nhập
              </Link>

              {/* CTA */}
              <Link
                href="/converter"
                id="header-cta-btn"
                className="flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-gradient-to-r from-[#2563EB] to-[#3B82F6] hover:from-[#1D4ED8] hover:to-[#2563EB] text-white text-sm font-semibold shadow-lg shadow-blue-500/25 hover:shadow-blue-500/40 transition-all duration-200 hover:scale-[1.02] active:scale-[0.98]"
              >
                <Zap size={14} strokeWidth={2.5} />
                <span className="hidden sm:inline">Bắt Đầu</span>
              </Link>

              {/* Mobile Menu Toggle */}
              <button
                id="header-mobile-menu-btn"
                onClick={() => setMobileOpen((v) => !v)}
                aria-label={mobileOpen ? "Đóng menu" : "Mở menu"}
                className="lg:hidden flex items-center justify-center w-9 h-9 rounded-lg bg-white/[0.05] hover:bg-white/[0.08] border border-white/[0.08] text-zinc-400 hover:text-[#FAFAFA] transition-all duration-200 cursor-pointer"
              >
                {mobileOpen ? <X size={18} /> : <Menu size={18} />}
              </button>
            </div>
          </div>
        </Container>

        {/* ── Mobile Menu ── */}
        <div
          className={`lg:hidden transition-all duration-300 overflow-hidden ${
            mobileOpen ? "max-h-[600px] opacity-100" : "max-h-0 opacity-0"
          }`}
        >
          <div className="bg-white/95 dark:bg-[#09090B]/95 backdrop-blur-xl border-b border-black/[0.06] dark:border-white/[0.06]">
            <Container>
              <div className="py-4 flex flex-col gap-1">
                {navLinks.map((link) => (
                  <div key={link.label}>
                    <Link
                      href={link.href}
                      onClick={() => setMobileOpen(false)}
                      className="flex items-center gap-2 px-3 py-2.5 text-sm font-medium text-zinc-400 hover:text-[#FAFAFA] hover:bg-white/[0.06] rounded-lg transition-all duration-150"
                    >
                      {link.label}
                    </Link>
                    {link.children && (
                      <div className="ml-4 border-l border-white/[0.06] pl-4 mt-1 mb-1 flex flex-col gap-0.5">
                        {link.children.map((child) => (
                          <Link
                            key={child.label}
                            href={child.href}
                            onClick={() => setMobileOpen(false)}
                            className="block px-3 py-2 text-sm text-zinc-500 hover:text-zinc-300 hover:bg-white/[0.06] rounded-lg transition-all duration-150"
                          >
                            {child.label}
                          </Link>
                        ))}
                      </div>
                    )}
                  </div>
                ))}

                {/* Mobile search */}
                <button
                  onClick={() => {
                    setMobileOpen(false);
                    setSearchOpen(true);
                  }}
                  className="flex items-center gap-2 px-3 py-2.5 text-sm font-medium text-zinc-500 hover:text-zinc-300 hover:bg-white/[0.06] rounded-lg transition-all duration-150 mt-2 border-t border-white/[0.06] pt-4 cursor-pointer"
                >
                  <Search size={14} />
                  Tìm kiếm bài nhạc...
                </button>

                {/* Mobile auth */}
                <div className="flex items-center gap-3 pt-2 border-t border-white/[0.06] mt-1">
                  <Link
                    href="/login"
                    onClick={() => setMobileOpen(false)}
                    className="flex-1 text-center py-2 text-sm font-medium text-zinc-400 hover:text-[#FAFAFA] border border-white/[0.08] rounded-lg hover:bg-white/[0.06] transition-all duration-150"
                  >
                    Đăng nhập
                  </Link>
                  <Link
                    href="/converter"
                    onClick={() => setMobileOpen(false)}
                    className="flex-1 text-center py-2 text-sm font-semibold text-white bg-gradient-to-r from-[#2563EB] to-[#3B82F6] rounded-lg hover:from-[#1D4ED8] hover:to-[#2563EB] transition-all duration-150"
                  >
                    Bắt Đầu Ngay
                  </Link>
                </div>
              </div>
            </Container>
          </div>
        </div>
      </header>

      {/* ── Search Modal ── */}
      {searchOpen && (
        <div
          className="fixed inset-0 z-[100] bg-black/70 backdrop-blur-sm flex items-start justify-center pt-24 px-4"
          onClick={() => setSearchOpen(false)}
        >
          <div
            className="w-full max-w-lg bg-white dark:bg-[#18181B] border border-black/[0.08] dark:border-white/[0.08] rounded-2xl shadow-2xl shadow-black/30 dark:shadow-black/60 overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-3 px-4 py-3.5 border-b border-white/[0.06]">
              <Search size={16} className="text-zinc-500 shrink-0" />
              <input
                id="search-modal-input"
                autoFocus
                type="text"
                placeholder="Tìm bài nhạc, tone, thể loại..."
                className="flex-1 bg-transparent text-[#FAFAFA] placeholder-zinc-600 text-sm outline-none"
              />
              <kbd className="text-[11px] text-zinc-600 bg-white/[0.05] px-2 py-1 rounded font-mono shrink-0">
                ESC
              </kbd>
            </div>
            <div className="px-4 py-8 text-center text-zinc-600 text-sm">
              Nhập từ khoá để tìm kiếm bài hát hoặc tone nhạc
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Header;