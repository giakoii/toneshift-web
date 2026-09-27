"use client";
import { ScrollReveal } from "./ScrollReveal";

export function SocialProof() {
  return (
    <div className="border-y border-border bg-background">
      <div className="mx-auto max-w-7xl px-6 md:px-8 py-8">
        <ScrollReveal>
          <div className="flex flex-col md:flex-row items-center justify-center gap-4 md:gap-8 text-center md:text-left text-muted-foreground font-medium text-sm md:text-base">
            <span className="flex items-center gap-2">
              <span className="text-primary">★</span> 4.9/5 Đánh giá
            </span>
            <span className="hidden md:inline text-border">•</span>
            <span>100,000+ Bài hát được chuyển tone</span>
            <span className="hidden md:inline text-border">•</span>
            <span className="text-foreground italic">"Công cụ cứu cánh cho các buổi diễn acoustic." — Indie Artist</span>
          </div>
        </ScrollReveal>
      </div>
    </div>
  );
}
