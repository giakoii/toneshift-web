"use client";

import { motion, useInView, useReducedMotion } from "framer-motion";
import { useRef } from "react";
import { Zap, Type, Palette, Music } from "lucide-react";

const FEATURES = [
  {
    icon: Zap,
    title: "Real-time",
    description:
      "Kết quả chuyển tone hiển thị ngay lập tức khi bạn gõ, không cần bấm nút.",
  },
  {
    icon: Type,
    title: "Giữ nguyên lời",
    description:
      "Thuật toán thông minh chỉ chuyển đổi nốt nhạc, lời ca được giữ nguyên.",
  },
  {
    icon: Palette,
    title: "Tô màu cú pháp",
    description:
      "Nốt nhạc và lời ca được tô màu khác nhau, dễ đọc và dễ theo dõi.",
  },
  {
    icon: Music,
    title: "Chuẩn nhạc lý",
    description:
      "Hỗ trợ đầy đủ 12 tone, hợp âm phức tạp và cả nốt nhạc Việt Nam.",
  },
];

export default function KeyFeatures() {
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { once: true, margin: "-60px" });
  const reduce = useReducedMotion();

  return (
    <section ref={ref} className="relative px-6 py-28 sm:py-36 lg:py-44">
      {/* Divider */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-white/[0.06] to-transparent"
      />

      {/* Split layout: header left, grid right on desktop */}
      <div className="mx-auto grid max-w-5xl grid-cols-1 gap-12 lg:grid-cols-[1fr_2fr] lg:gap-16">
        {/* Left: section header */}
        <div className="lg:sticky lg:top-32 lg:self-start">
          <h2 className="text-2xl font-bold text-white sm:text-3xl">
            Tại sao chọn ToneShift?
          </h2>
          <p className="mt-3 max-w-xs text-sm leading-relaxed text-white/40">
            Được thiết kế tập trung vào trải nghiệm người dùng, ToneShift giúp
            bạn chuyển đổi cảm âm một cách trực quan nhất.
          </p>
        </div>

        {/* Right: feature grid */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {FEATURES.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={reduce ? false : { opacity: 0, y: 32 }}
              animate={inView ? { opacity: 1, y: 0 } : {}}
              transition={{
                duration: 0.5,
                delay: reduce ? 0 : i * 0.1,
                ease: [0.16, 1, 0.3, 1],
              }}
              className="group relative rounded-2xl border border-white/[0.06] bg-white/[0.02] p-6 transition-colors duration-300 hover:border-amber-500/20 hover:bg-white/[0.04]"
            >
              <div className="flex items-center gap-3 mb-3">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-500/10 text-amber-400 transition-colors duration-300 group-hover:bg-amber-500/15">
                  <feature.icon className="h-4 w-4" />
                </div>
                <h3 className="text-sm font-semibold text-white">
                  {feature.title}
                </h3>
              </div>
              <p className="text-[13px] leading-relaxed text-white/40">
                {feature.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
