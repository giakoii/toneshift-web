"use client";
import Link from "next/link";
import { ScrollReveal } from "./ScrollReveal";
import { AuroraBackground } from "./AuroraBackground";

export function FinalCTA() {
  return (
    <section className="relative py-32 md:py-48 overflow-hidden flex items-center justify-center border-t border-border/50 bg-background">
      <div className="absolute inset-0 opacity-50">
        <AuroraBackground />
      </div>
      
      <div className="relative z-10 mx-auto max-w-3xl px-6 md:px-8 text-center">
        <ScrollReveal>
          <h2 className="text-[clamp(40px,5vw,72px)] font-bold tracking-[-0.04em] leading-tight mb-8">
            Bắt đầu chơi đúng tone của bạn.
          </h2>
          
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-8">
            <Link href="/converter" className="w-full sm:w-auto flex items-center justify-center h-14 px-10 rounded-full bg-primary text-primary-foreground font-semibold hover:brightness-110 transition-all">
              Thử nghiệm miễn phí
            </Link>
          </div>
          
          <p className="text-muted-foreground font-light">
            Sử dụng trực tiếp trên trình duyệt. Không cần đăng nhập.
          </p>
        </ScrollReveal>
      </div>
    </section>
  );
}
