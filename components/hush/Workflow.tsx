"use client";
import { ScrollReveal } from "./ScrollReveal";
import { motion, useScroll, useTransform } from "framer-motion";
import { useRef } from "react";

export function Workflow() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start center", "end center"],
  });

  const scaleY = useTransform(scrollYProgress, [0, 0.8], [0, 1]);

  const routine = [
    { step: "01", title: "Copy lời bài hát", desc: "Tìm và copy bài hát yêu thích của bạn từ bất kỳ trang hợp âm hay website âm nhạc nào." },
    { step: "02", title: "Dán vào ToneShift", desc: "Trình phân tích của ToneShift tự động tách biệt giữa lời bài hát tiếng Việt và các ký hiệu hợp âm." },
    { step: "03", title: "Chọn tone phù hợp", desc: "Giọng bạn hợp với tone G hơn tone C? Chỉ cần một click, toàn bộ hợp âm sẽ được tính toán lại." },
    { step: "04", title: "Chơi nhạc", desc: "Bật chế độ biểu diễn với tự động cuộn và giữ màn hình sáng, giúp bạn vừa đàn vừa hát dễ dàng." },
  ];

  return (
    <section className="py-24 md:py-32 overflow-hidden bg-card/30" id="workflow">
      <div className="mx-auto max-w-4xl px-6 md:px-8">
        <ScrollReveal>
          <div className="mb-16 md:mb-24 text-center">
            <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em]">Bốn bước để có cảm âm hoàn hảo.</h2>
          </div>
        </ScrollReveal>

        <div className="relative pl-6 md:pl-8 py-4" ref={containerRef}>
          <div className="absolute left-0 top-0 bottom-0 w-px bg-border"></div>
          
          <motion.div 
            className="absolute left-0 top-0 bottom-0 w-px origin-top bg-gradient-to-b from-primary via-accent to-background"
            style={{ scaleY }}
          ></motion.div>

          <div className="flex flex-col gap-12 md:gap-16">
            {routine.map((item, i) => (
              <ScrollReveal key={i} delay={0.1}>
                <div className="relative">
                  <div className="absolute -left-[29px] md:-left-[37px] top-1.5 w-3 h-3 rounded-full bg-background border-2 border-primary"></div>
                  
                  <div className="flex flex-col md:flex-row md:items-start gap-2 md:gap-8">
                    <div className="text-primary font-mono font-bold text-xl tracking-widest pt-0.5 md:w-20 shrink-0">
                      {item.step}
                    </div>
                    <div>
                      <h3 className="text-xl font-bold text-foreground mb-2">{item.title}</h3>
                      <p className="text-muted-foreground font-light leading-[1.7] text-lg max-w-xl">{item.desc}</p>
                    </div>
                  </div>
                </div>
              </ScrollReveal>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
