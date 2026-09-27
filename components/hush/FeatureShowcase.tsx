"use client";
import { ScrollReveal } from "./ScrollReveal";
import { motion } from "framer-motion";
import { clsx } from "clsx";

export function FeatureShowcase() {
  const features = [
    {
      title: "Sleep stories, read slowly",
      desc: "Forty stories recorded at a deliberate pace by people with unhurried voices. Most listeners never hear the ending.",
      align: "right",
    },
    {
      title: "Breathing that matches your night",
      desc: "Pick four minutes or twelve. The circle on screen dims as you go, so it never becomes the bright thing in a dark room.",
      align: "left",
    },
    {
      title: "A wind-down that learns your bedtime",
      desc: "Hush notices when you usually start and quietly gets ready, without asking you to set a schedule.",
      align: "right",
    },
  ];

  return (
    <section className="py-24 md:py-32 bg-background overflow-hidden">
      <div className="mx-auto max-w-7xl px-6 md:px-8 flex flex-col gap-32">
        {features.map((feature, i) => (
          <div key={i} className={clsx("flex flex-col md:flex-row items-center gap-12 lg:gap-24", feature.align === "left" ? "md:flex-row-reverse" : "")}>
            <div className="flex-1 w-full">
              <ScrollReveal>
                <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em] leading-tight mb-6">{feature.title}</h2>
                <p className="text-lg text-muted-foreground font-light leading-[1.7] max-w-md">{feature.desc}</p>
              </ScrollReveal>
            </div>
            
            <div className="flex-1 w-full flex justify-center perspective-[1000px]">
              <ScrollReveal delay={0.2}>
                <motion.div 
                  animate={{ y: [0, -10, 0] }}
                  transition={{ duration: 5 + i, repeat: Infinity, ease: "easeInOut", delay: i * 0.5 }}
                  className="w-[260px] h-[540px] md:w-[300px] md:h-[620px] rounded-[2.5rem] border-[6px] border-border bg-[#0B0A1A] overflow-hidden relative shadow-[0_0_40px_rgba(0,0,0,0.5)]"
                >
                  {/* Mock content based on the feature index */}
                  <div className="absolute inset-0 p-6 flex flex-col items-center justify-center">
                    {i === 0 && (
                      <div className="w-full flex flex-col items-center">
                        <div className="w-24 h-32 rounded-lg bg-card border border-border mb-8"></div>
                        <div className="w-3/4 h-2 bg-foreground/20 rounded-full mb-4">
                          <div className="w-1/3 h-full bg-primary rounded-full"></div>
                        </div>
                        <div className="flex justify-between w-3/4 text-[10px] text-muted-foreground mb-8">
                          <span>12:00</span><span>45:00</span>
                        </div>
                        <div className="flex items-center gap-6">
                          <div className="w-8 h-8 rounded-full bg-card"></div>
                          <div className="w-16 h-16 rounded-full bg-foreground flex items-center justify-center text-background text-2xl">▶</div>
                          <div className="w-8 h-8 rounded-full bg-card"></div>
                        </div>
                      </div>
                    )}
                    {i === 1 && (
                      <div className="w-40 h-40 rounded-full border-[2px] border-primary/20 flex items-center justify-center relative">
                        <motion.div 
                          animate={{ scale: [1, 1.5, 1], opacity: [0.2, 0.05, 0.2] }}
                          transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
                          className="absolute inset-0 rounded-full bg-primary/30"
                        />
                        <div className="text-primary font-light text-2xl">Breathe</div>
                      </div>
                    )}
                    {i === 2 && (
                      <div className="w-full flex flex-col items-center text-center">
                        <div className="text-primary text-sm uppercase tracking-widest mb-4">9:45 PM</div>
                        <h3 className="text-foreground text-2xl font-light mb-2">Getting ready<br/>for bed</h3>
                        <p className="text-muted-foreground text-sm max-w-[80%]">Screen dimmed, notifications paused.</p>
                      </div>
                    )}
                  </div>
                </motion.div>
              </ScrollReveal>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
