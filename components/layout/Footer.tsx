import Link from "next/link";
import Logo from "@/components/ui/Logo";

export default function Footer() {
  return (
    <footer className="w-full bg-[oklch(0.14_0.015_45)] pb-12 pt-24 text-center">
      <div className="mx-auto flex max-w-4xl flex-col items-center px-6">
        <Logo />
        <h2 className="mt-8 max-w-xl text-3xl font-bold tracking-tight text-white sm:text-4xl">
          Công cụ chuẩn xác cho<br className="hidden sm:block" /> người yêu nhạc cụ.
        </h2>
        <p className="mt-4 max-w-md text-sm leading-relaxed text-white/40">
          ToneShift giúp bạn chuyển đổi cảm âm nhanh chóng, 
          hỗ trợ đầy đủ 12 tone và giữ nguyên lời ca.
        </p>

        <div className="mt-12 flex flex-col items-center gap-6 sm:flex-row sm:gap-8">
          <Link
            href="/converter"
            className="group inline-flex items-center gap-2 rounded-xl bg-white/[0.05] px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-white/[0.1]"
          >
            Chuyển tone ngay
          </Link>
          <Link
            href="/community"
            className="text-sm font-medium text-white/50 transition-colors hover:text-white"
          >
            Khám phá thư viện
          </Link>
        </div>

        <div className="mt-20 flex w-full flex-col items-center justify-between gap-4 border-t border-white/[0.05] pt-8 sm:flex-row text-sm text-white/30">
          <div className="flex gap-6">
            <Link href="/terms" className="hover:text-white transition-colors">Điều khoản</Link>
            <Link href="/privacy" className="hover:text-white transition-colors">Bảo mật</Link>
          </div>
          <p>&copy; {new Date().getFullYear()} ToneShift.</p>
        </div>
      </div>
    </footer>
  );
}