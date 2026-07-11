import Link from "next/link";
import Logo from "@/components/ui/Logo";

const FOOTER_LINKS = {
  "Sản phẩm": [
    { label: "Chuyển đổi cảm âm", href: "/converter" },
    { label: "Thư viện của tôi", href: "/library" },
    { label: "Cộng đồng", href: "/community" },
    { label: "Blog", href: "/blog" },
  ],
  "Hỗ trợ": [
    { label: "Hướng dẫn sử dụng", href: "#how-it-works" },
    { label: "Câu hỏi thường gặp", href: "/faq" },
    { label: "Liên hệ", href: "/contact" },
  ],
  "Pháp lý": [
    { label: "Điều khoản dịch vụ", href: "/terms" },
    { label: "Chính sách bảo mật", href: "/privacy" },
  ],
};

export default function Footer() {
  return (
    <footer className="relative w-full border-t border-white/5 bg-[oklch(0.09_0.002_264)]">
      {/* Ambient glow top */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px bg-linear-to-r from-transparent via-blue-500/30 to-transparent"
      />

      <div className="mx-auto w-full max-w-6xl px-6 py-16">
        {/* Main grid: logo left + links right */}
        <div className="grid grid-cols-1 gap-12 md:grid-cols-[1.2fr_2fr]">

          {/* ── Left: Brand ── */}
          <div className="flex flex-col gap-5">
            <Logo />
            <p className="max-w-65 text-sm leading-relaxed text-white/40">
              Công cụ chuyển đổi cảm âm trực tuyến — nhanh, chính xác và miễn phí cho mọi người.
            </p>
          </div>

          {/* ── Right: Link columns ── */}
          <div className="grid grid-cols-2 gap-8 sm:grid-cols-3">
            {Object.entries(FOOTER_LINKS).map(([group, links]) => (
              <div key={group} className="flex flex-col gap-3">
                <p className="text-xs font-semibold uppercase tracking-widest text-white/25">
                  {group}
                </p>
                <ul className="flex flex-col gap-2.5" role="list">
                  {links.map(({ label, href }) => (
                    <li key={href}>
                      <Link
                        href={href}
                        className="text-sm text-white/50 hover:text-white transition-colors duration-200"
                      >
                        {label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        {/* ── Bottom bar ── */}
        <div className="mt-14 flex flex-col items-center justify-between gap-4 border-t border-white/5 pt-8 sm:flex-row">
          <p className="text-xs text-white/25">
            &copy; {new Date().getFullYear()} ToneShift. Bảo lưu mọi quyền.
          </p>
        </div>
      </div>
    </footer>
  );
}