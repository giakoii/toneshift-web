"use client";
import { ScrollReveal } from "./ScrollReveal";
import { motion, useScroll, useTransform } from "framer-motion";
import { useRef } from "react";

export function NightRoutine() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start center", "end center"],
  });

  const scaleY = useTransform(scrollYProgress, [0, 0.8], [0, 1]);

  const routine = [
    { time: "21:00", title: "Dim everything", desc: "Hush switches to a red-shifted screen and drops the interface brightness." },
    { time: "21:45", title: "Wind down", desc: "A four-minute breathing session, or a stretch you can do in bed." },
    { time: "22:30", title: "Story or sound", desc: "Pick one and set a fade. Most people choose thirty minutes." },
    { time: "23:00", title: "Nothing", desc: "The screen goes black and stays that way until morning." },
  ];

  return (
    <section className="py-24 md:py-32 overflow-hidden">
      <div className="mx-auto max-w-4xl px-6 md:px-8">
        <ScrollReveal>
          <div className="mb-16 md:mb-24 text-center">
            <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em]">Your last two hours, made easier.</h2>
          </div>
        </ScrollReveal>

        <div className="relative pl-6 md:pl-8 py-4" ref={containerRef}>
          {/* Background line */}
          <div className="absolute left-0 top-0 bottom-0 w-px bg-border"></div>
          
          {/* Animated gradient line */}
          <motion.div 
            className="absolute left-0 top-0 bottom-0 w-px origin-top bg-gradient-to-b from-primary via-accent to-background"
            style={{ scaleY }}
          ></motion.div>

          <div className="flex flex-col gap-12 md:gap-16">
            {routine.map((step, i) => (
              <ScrollReveal key={i} delay={0.1}>
                <div className="relative">
                  {/* Dot */}
                  <div className="absolute -left-[29px] md:-left-[37px] top-1.5 w-3 h-3 rounded-full bg-background border-2 border-primary"></div>
                  
                  <div className="flex flex-col md:flex-row md:items-start gap-2 md:gap-8">
                    <div className="text-primary font-mono font-medium text-lg tracking-widest pt-0.5 md:w-24 shrink-0">
                      {step.time}
                    </div>
                    <div>
                      <h3 className="text-xl font-bold text-foreground mb-2">{step.title}</h3>
                      <p className="text-muted-foreground font-light leading-[1.7] text-lg max-w-xl">{step.desc}</p>
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
