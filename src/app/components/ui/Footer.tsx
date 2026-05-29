'use client';
import React from "react";
import Link from "next/link";
import Container from "@/app/components/Container";
import {
  Music2,
  Mail,
  Heart,
  ExternalLink,
} from "lucide-react";

const footerLinks = {
  "Tính Năng": [
    { label: "Chuyển Tone", href: "/converter" },
    { label: "Phát Hiện Tone Tự Động", href: "/tools/detect" },
    { label: "So Sánh Tone", href: "/tools/compare" },
    { label: "Luyện Tập", href: "/tools/practice" },
  ],
  "Thư Viện": [
    { label: "Tất Cả Bài Nhạc", href: "/library" },
    { label: "Bolero & Cải Lương", href: "/library?genre=bolero" },
    { label: "Nhạc Trẻ", href: "/library?genre=nhac-tre" },
    { label: "Sáo Trúc", href: "/library?genre=sao-truc" },
  ],
  "Cộng Đồng": [
    { label: "Đăng Bài", href: "/community/upload" },
    { label: "Diễn Đàn", href: "/community" },
    { label: "Đóng Góp", href: "/contribute" },
    { label: "Blog", href: "/blog" },
  ],
  "Hỗ Trợ": [
    { label: "Hướng Dẫn Sử Dụng", href: "/docs" },
    { label: "Câu Hỏi Thường Gặp", href: "/faq" },
    { label: "Liên Hệ", href: "/contact" },
    { label: "Báo Lỗi", href: "/report" },
  ],
};

const genres = [
  "Bolero",
  "Cải Lương",
  "Sáo Trúc",
  "Harmonica",
  "Guitar",
  "Nhạc Trẻ",
  "Dân Ca",
];

const socialLinks = [
  {
    icon: Mail,
    label: "Email",
    href: "mailto:hello@toneshift.vn",
    color: "hover:text-[#60A5FA]",
  },
];

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="relative bg-zinc-50 dark:bg-[#09090B] border-t border-black/[0.06] dark:border-white/[0.06] mt-auto">
      {/* Gradient glow top */}
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-[#2563EB]/40 to-transparent" />

      <Container>
        {/* ── Main grid ── */}
        <div className="pt-16 pb-10 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-10">
          {/* Brand column */}
          <div className="lg:col-span-2 flex flex-col gap-5">
            {/* Logo */}
            <Link
              href="/"
              className="flex items-center gap-2.5 w-fit group select-none"
            >
              <div className="relative w-9 h-9 rounded-xl bg-gradient-to-br from-[#2563EB] to-[#60A5FA] flex items-center justify-center shadow-lg shadow-blue-500/30 group-hover:shadow-blue-500/50 transition-all duration-300">
                <Music2 size={18} className="text-white" strokeWidth={2.5} />
                <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-white/20 to-transparent" />
              </div>
              <div className="flex flex-col leading-none">
                <span className="text-[#FAFAFA] font-bold text-xl tracking-tight">
                  ToneShift
                </span>
                <span className="text-[#60A5FA] text-[10px] font-medium tracking-widest uppercase">
                  Cảm Âm Pro
                </span>
              </div>
            </Link>

            {/* Description */}
            <p className="text-zinc-500 text-sm leading-relaxed max-w-xs">
              Công cụ chuyển đổi cảm âm nhạc Việt Nam hàng đầu. Hỗ trợ sáo
              trúc, harmonica, guitar và nhiều nhạc cụ khác.
            </p>

            {/* Genre tags */}
            <div className="flex flex-wrap gap-1.5">
              {genres.map((g) => (
                <span
                  key={g}
                  className="inline-block px-2.5 py-0.5 rounded-full text-[11px] font-medium text-zinc-500 bg-white/[0.04] border border-white/[0.06] hover:text-[#60A5FA] hover:border-[#2563EB]/40 transition-colors duration-200 cursor-default"
                >
                  {g}
                </span>
              ))}
            </div>

            {/* Social links */}
            <div className="flex items-center gap-2 pt-1">
              {socialLinks.map(({ icon: Icon, label, href, color }) => (
                <a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={label}
                  className={`w-9 h-9 flex items-center justify-center rounded-lg bg-white/[0.04] border border-white/[0.06] text-zinc-600 ${color} hover:bg-white/[0.08] hover:border-white/[0.12] transition-all duration-200`}
                >
                  <Icon size={16} />
                </a>
              ))}
            </div>
          </div>

          {/* Link columns */}
          {Object.entries(footerLinks).map(([title, links]) => (
            <div key={title} className="flex flex-col gap-4">
              <h3 className="text-[#FAFAFA] text-sm font-semibold tracking-wide">
                {title}
              </h3>
              <ul className="flex flex-col gap-2.5">
                {links.map((link) => (
                  <li key={link.label}>
                    <Link
                      href={link.href}
                      className="text-sm text-zinc-500 hover:text-zinc-300 transition-colors duration-150 flex items-center gap-1 group"
                    >
                      {link.label}
                      {link.href.startsWith("http") && (
                        <ExternalLink
                          size={11}
                          className="opacity-0 group-hover:opacity-100 transition-opacity"
                        />
                      )}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* ── Newsletter ── */}
        <div className="py-8 border-t border-white/[0.06]">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <p className="text-[#FAFAFA] text-sm font-semibold">
                Nhận thông báo bài nhạc mới
              </p>
              <p className="text-zinc-500 text-xs mt-0.5">
                Cập nhật hàng tuần. Không spam.
              </p>
            </div>
            <form
              className="flex items-center gap-2 w-full sm:w-auto"
              onSubmit={(e) => e.preventDefault()}
            >
              <input
                id="footer-newsletter-email"
                type="email"
                placeholder="email@example.com"
                className="flex-1 sm:w-60 px-4 py-2 rounded-lg bg-white/[0.05] border border-white/[0.08] text-sm text-[#FAFAFA] placeholder-zinc-600 outline-none focus:border-[#2563EB]/60 focus:bg-white/[0.08] transition-all duration-200"
              />
              <button
                type="submit"
                id="footer-newsletter-submit"
                className="px-4 py-2 rounded-lg bg-gradient-to-r from-[#2563EB] to-[#3B82F6] hover:from-[#1D4ED8] hover:to-[#2563EB] text-white text-sm font-semibold transition-all duration-200 whitespace-nowrap hover:shadow-lg hover:shadow-blue-500/25 cursor-pointer"
              >
                Đăng Ký
              </button>
            </form>
          </div>
        </div>

        {/* ── Bottom bar ── */}
        <div className="py-6 border-t border-white/[0.06] flex flex-col sm:flex-row items-center justify-between gap-3">
          <p className="text-zinc-600 text-xs flex items-center gap-1.5">
            © {currentYear} ToneShift. Được làm với{" "}
            <Heart
              size={11}
              className="text-red-500 fill-red-500 inline"
              aria-label="tình yêu"
            />{" "}
            tại Việt Nam.
          </p>

          <div className="flex items-center gap-4">
            <Link
              href="/privacy"
              className="text-xs text-zinc-600 hover:text-zinc-400 transition-colors duration-150"
            >
              Chính Sách Bảo Mật
            </Link>
            <span className="text-zinc-800">·</span>
            <Link
              href="/terms"
              className="text-xs text-zinc-600 hover:text-zinc-400 transition-colors duration-150"
            >
              Điều Khoản
            </Link>
            <span className="text-zinc-800">·</span>
            <Link
              href="/sitemap"
              className="text-xs text-zinc-600 hover:text-zinc-400 transition-colors duration-150"
            >
              Sitemap
            </Link>
          </div>
        </div>
      </Container>
    </footer>
  );
};

export default Footer;