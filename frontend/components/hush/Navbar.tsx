"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { Music } from "lucide-react";
import { clsx } from "clsx";

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 85);
    };
    handleScroll();
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <nav
      className={clsx(
        "fixed top-0 left-0 right-0 z-50 h-[76px] transition-all duration-300 px-6 md:px-8",
        scrolled ? "bg-background/85 backdrop-blur-md border-b border-border" : "bg-transparent"
      )}
    >
      <div className="mx-auto max-w-7xl h-full flex items-center justify-between">
        <Link href="/" className="flex items-center gap-2 text-foreground" aria-label="ToneShift – Trang chủ">
          <Music className="w-5 h-5 text-primary" />
          <span className="font-bold tracking-tight text-xl">ToneShift</span>
        </Link>

        <div className="hidden md:flex items-center gap-8 text-sm font-medium text-muted-foreground">
          <Link href="/#features" className="hover:text-foreground transition-colors">Tính năng</Link>
          <Link href="/#converter" className="hover:text-foreground transition-colors">Thử nghiệm</Link>
          <Link href="/#workflow" className="hover:text-foreground transition-colors">Hướng dẫn</Link>
        </div>

        <Link
          href="/converter"
          className="inline-flex items-center h-10 px-5 rounded-full bg-primary text-primary-foreground font-semibold text-sm hover:brightness-110 transition-all"
        >
          <span className="hidden md:inline">Chuyển Tone Ngay</span>
          <span className="md:hidden">Bắt đầu</span>
        </Link>
      </div>
    </nav>
  );
}
