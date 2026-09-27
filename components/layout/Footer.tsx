import Link from "next/link";
import Logo from "@/components/ui/Logo";

export default function Footer() {
  return (
    <footer className="relative w-full border-t border-white/[0.06] bg-[#0c0a09]">
      {/* Subtle top glow to separate from the page content */}
      <div
        aria-hidden="true"
        className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-amber-500/20 to-transparent"
      />

      <div className="mx-auto flex w-full max-w-5xl flex-col items-center justify-between gap-6 px-6 py-8 sm:flex-row">
        {/* Left: Logo & Copyright */}
        <div className="flex flex-col items-center gap-2 sm:items-start">
          <Logo />
          <p className="text-xs text-white/40">
            &copy; {new Date().getFullYear()} ToneShift. Chuyển cảm âm nhanh chóng.
          </p>
        </div>

        {/* Right: Links */}
        <div className="flex items-center gap-6 text-sm font-medium text-white/50">
          <Link
            href="/converter"
            className="transition-colors hover:text-amber-400"
          >
            Công cụ
          </Link>
          <Link
            href="/terms"
            className="transition-colors hover:text-white"
          >
            Điều khoản
          </Link>
          <Link
            href="/privacy"
            className="transition-colors hover:text-white"
          >
            Bảo mật
          </Link>
        </div>
      </div>
    </footer>
  );
}