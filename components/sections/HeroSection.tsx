"use client";

import { motion, useReducedMotion } from "framer-motion";
import { ArrowRight } from "lucide-react";
import Link from "next/link";
import { TextReveal } from "@/components/effects/TextReveal";
import { FloatingNotes } from "@/components/effects/FloatingNotes";
import { MagneticWrapper } from "@/components/effects/MagneticWrapper";

export default function HeroSection() {
  const reduce = useReducedMotion();

  return (
    <section className="relative flex min-h-[calc(100dvh-5rem)] items-center justify-center overflow-hidden px-6 pt-16 pb-28 sm:pt-20 sm:pb-36 lg:pt-24 lg:pb-44">
      {/* Background Floating Notes */}
      <FloatingNotes />

      <div className="relative z-10 mx-auto max-w-4xl text-center">
        {/* Badge */}
        <motion.div
          initial={reduce ? false : { opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-6 inline-flex items-center rounded-full border border-white/[0.08] bg-white/[0.04] px-4 py-1.5 text-xs font-medium text-white/50 backdrop-blur-sm"
        >
          Miễn phí, không cần đăng nhập
        </motion.div>

        {/* Heading with TextReveal */}
        <h1 className="text-4xl font-bold leading-[1.1] tracking-tight text-white sm:text-5xl lg:text-6xl flex flex-wrap justify-center">
          <TextReveal delay={0.1}>Chuyển tone cảm âm</TextReveal>
          <motion.span
            initial={reduce ? false : { opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{
              duration: 0.6,
              delay: reduce ? 0 : 0.4, // delayed after the first part
              type: "spring" as const,
              damping: 12,
              stiffness: 100,
            }}
            className="text-amber-400 mx-2"
          >
            nhanh chóng
          </motion.span>
          <TextReveal delay={0.5}>và chuẩn xác</TextReveal>
        </h1>

        {/* Subtitle */}
        <motion.p
          initial={reduce ? false : { opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: reduce ? 0 : 0.8 }}
          className="mx-auto mt-6 max-w-xl text-base leading-relaxed text-white/50 sm:text-lg"
        >
          Dán cảm âm vào, chọn tone gốc và tone đích. Kết quả hiển thị ngay lập
          tức. Hỗ trợ cả nốt Tây phương và nốt nhạc Việt.
        </motion.p>

        {/* CTA with Magnetic Effect */}
        <motion.div
          initial={reduce ? false : { opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={reduce ? {} : { duration: 0.5, delay: 1, type: "spring", stiffness: 200 }}
          className="mt-10 flex justify-center"
        >
          <MagneticWrapper>
            <Link
              href="/converter"
              id="hero-cta"
              className="group inline-flex items-center gap-2.5 rounded-xl bg-amber-600 px-7 py-3.5 text-sm font-semibold text-white transition-colors duration-300 hover:bg-amber-500 active:scale-[0.98]"
            >
              Dùng thử ngay
              <ArrowRight className="h-4 w-4 transition-transform duration-300 group-hover:translate-x-1" />
            </Link>
          </MagneticWrapper>
        </motion.div>
      </div>
    </section>
  );
}
