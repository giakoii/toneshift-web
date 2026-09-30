"use client";
import Link from "next/link";
import { motion, useScroll, useTransform } from "framer-motion";
import { useRef } from "react";
import { AuroraBackground } from "./AuroraBackground";
import { StarField } from "./StarField";

export function HeroSection() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start start", "end start"],
  });

  const rotateX = useTransform(scrollYProgress, [0, 1], [0, 14]);
  const rotateY = useTransform(scrollYProgress, [0, 1], [-14, 0]);
  const rotateZ = useTransform(scrollYProgress, [0, 1], [3, 0]);

  return (
    <section ref={containerRef} className="relative min-h-[100dvh] pt-24 pb-16 flex items-center overflow-hidden">
      <AuroraBackground />
      <StarField />
      
      <div className="relative z-10 mx-auto max-w-7xl w-full px-6 md:px-8 grid grid-cols-1 md:grid-cols-2 gap-12 lg:gap-24 items-center">
        {/* Left: Typography */}
        <div className="flex flex-col items-start order-2 md:order-1">
          <motion.div
            initial={{ opacity: 0, y: 40, filter: "blur(8px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            transition={{ duration: 0.9, delay: 0.1 }}
            className="text-[12px] font-medium uppercase tracking-[0.16em] text-primary mb-6"
          >
            SMART TONE CONVERTER
          </motion.div>
          
          <h1 className="text-[clamp(46px,6.5vw,96px)] font-bold leading-[1.02] tracking-[-0.04em] mb-8">
            <motion.span
              initial={{ opacity: 0, y: 40, filter: "blur(8px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{ duration: 0.9, delay: 0.2 }}
              className="block"
            >
              Đổi tone bài hát.
            </motion.span>
            <motion.span
              initial={{ opacity: 0, y: 40, filter: "blur(8px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{ duration: 0.9, delay: 0.3 }}
              className="block text-muted-foreground"
            >
              Nhanh chóng.
            </motion.span>
            <motion.span
              initial={{ opacity: 0, y: 40, filter: "blur(8px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{ duration: 0.9, delay: 0.4 }}
              className="block"
            >
              Chuẩn xác.
            </motion.span>
          </h1>
          
          <motion.p
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.9, delay: 0.5 }}
            className="text-lg text-muted-foreground font-light leading-[1.7] max-w-md mb-10"
          >
            Dán hợp âm hoặc cảm âm của bạn vào đây. ToneShift sẽ tự động phân tích và chuyển đổi sang tone bạn muốn chỉ trong tích tắc.
          </motion.p>
          
          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.9, delay: 0.6 }}
            className="flex flex-col sm:flex-row items-center gap-4 w-full sm:w-auto"
          >
            <Link href="/converter" className="w-full sm:w-auto flex items-center justify-center h-14 px-8 rounded-full bg-primary text-primary-foreground font-semibold hover:brightness-110 transition-all">
              Chuyển Tone Ngay
            </Link>
            <Link href="/#features" className="w-full sm:w-auto flex items-center justify-center h-14 px-8 rounded-full bg-card border border-border text-foreground font-semibold hover:bg-card/80 transition-colors">
              Tìm hiểu thêm
            </Link>
          </motion.div>
        </div>

        {/* Right: Floating Converter UI Mockup */}
        <div className="order-1 md:order-2 h-[52vh] md:h-auto flex justify-center perspective-[1500px]">
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 1.2, delay: 0.4 }}
            className="relative w-full max-w-[400px]"
            style={{ rotateX, rotateY, rotateZ }}
          >
            <motion.div
              animate={{ y: [0, -12, 0] }}
              transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
              className="w-full rounded-2xl border-[1px] border-border bg-card overflow-hidden shadow-2xl relative flex flex-col"
            >
              {/* Mockup Header */}
              <div className="h-12 bg-background border-b border-border flex items-center px-4 justify-between">
                <div className="flex gap-2">
                  <div className="w-3 h-3 rounded-full bg-border"></div>
                  <div className="w-3 h-3 rounded-full bg-border"></div>
                  <div className="w-3 h-3 rounded-full bg-border"></div>
                </div>
                <div className="flex gap-2 items-center">
                  <div className="text-[10px] uppercase font-bold text-muted-foreground bg-background px-2 py-1 rounded">C</div>
                  <span className="text-muted-foreground text-xs">→</span>
                  <div className="text-[10px] uppercase font-bold text-primary bg-primary/10 border border-primary/20 px-2 py-1 rounded">G</div>
                </div>
              </div>
              
              {/* Mockup Body */}
              <div className="flex-1 flex flex-col p-4 bg-gradient-to-b from-card to-background">
                <div className="mb-4">
                  <div className="text-xs text-muted-foreground mb-2">Original (C)</div>
                  <div className="font-mono text-xs text-foreground/60 leading-relaxed whitespace-pre">
                    <span className="text-foreground font-bold">C</span>       <span className="text-foreground font-bold">G</span>       <span className="text-foreground font-bold">Am</span>      <span className="text-foreground font-bold">F</span>{"\n"}
                    Cho em một nhành hoa vương
                  </div>
                </div>
                
                <div className="h-px w-full bg-border mb-4"></div>
                
                <div>
                  <div className="text-xs text-primary mb-2">Transposed (G)</div>
                  <div className="font-mono text-xs text-foreground/80 leading-relaxed whitespace-pre">
                    <span className="text-primary font-bold">G</span>       <span className="text-primary font-bold">D</span>       <span className="text-primary font-bold">Em</span>      <span className="text-primary font-bold">C</span>{"\n"}
                    Cho em một nhành hoa vương
                  </div>
                </div>
              </div>
            </motion.div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
