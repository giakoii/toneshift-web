"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { Music, LogOut, User as UserIcon } from "lucide-react";
import { clsx } from "clsx";
import { useAuth } from "@/components/auth/AuthProvider";
import { logoutAction } from "@/app/(auth)/actions";

export function Navbar() {
  const { user } = useAuth();
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

        {user ? (
          <div className="flex items-center gap-3">
            <Link
              href="/converter"
              className="hidden sm:inline-flex items-center h-10 px-4 rounded-full bg-primary text-primary-foreground font-semibold text-sm hover:brightness-110 transition-all"
            >
              Chuyển Tone
            </Link>
            <UserMenu name={user.name} image={user.image} />
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <Link href="/login" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
              Đăng nhập
            </Link>
            <Link
              href="/register"
              className="inline-flex items-center h-10 px-5 rounded-full bg-primary text-primary-foreground font-semibold text-sm hover:brightness-110 transition-all"
            >
              Đăng ký
            </Link>
          </div>
        )}
      </div>
    </nav>
  );
}

function UserMenu({ name, image }: { name: string; image: string | null }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2 h-10 pl-1 pr-3 rounded-full border border-border hover:bg-card transition-colors cursor-pointer"
      >
        {image ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={image} alt={name} className="w-8 h-8 rounded-full object-cover" />
        ) : (
          <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center">
            <UserIcon className="w-4 h-4 text-primary" />
          </div>
        )}
        <span className="hidden md:inline text-sm font-medium text-foreground max-w-[120px] truncate">
          {name}
        </span>
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute right-0 mt-2 w-48 rounded-xl border border-border bg-card shadow-2xl z-50 py-1">
            <form action={logoutAction}>
              <button
                type="submit"
                className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-foreground hover:bg-background/60 transition-colors cursor-pointer text-left"
              >
                <LogOut className="w-4 h-4" />
                Đăng xuất
              </button>
            </form>
          </div>
        </>
      )}
    </div>
  );
}
