"use client";
import { ScrollReveal } from "./ScrollReveal";
import { CloudRain, Waves, Trees, CloudLightning, Flame, Snowflake, Fan, Train } from "lucide-react";

export function SoundLibrary() {
  const sounds = [
    { name: "Rain on a tent", duration: "45 min", icon: CloudRain },
    { name: "Slow ocean", duration: "60 min", icon: Waves },
    { name: "Night forest", duration: "45 min", icon: Trees },
    { name: "Distant thunder", duration: "30 min", icon: CloudLightning },
    { name: "Cabin fire", duration: "60 min", icon: Flame },
    { name: "Snowfall", duration: "45 min", icon: Snowflake },
    { name: "Old fan", duration: "90 min", icon: Fan },
    { name: "Train at night", duration: "60 min", icon: Train },
  ];

  return (
    <section className="py-24 md:py-32 bg-card/30" id="sounds">
      <div className="mx-auto max-w-7xl px-6 md:px-8">
        <ScrollReveal>
          <div className="mb-16 md:mb-20 text-center md:text-left">
            <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em] mb-4">Eight sounds. That's all we need.</h2>
          </div>
        </ScrollReveal>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-6">
          {sounds.map((sound, i) => (
            <ScrollReveal key={i} delay={i * 0.05}>
              <div className="group relative flex flex-col items-center justify-center p-8 rounded-3xl bg-card border border-border hover:border-primary/50 transition-all duration-250 hover:-translate-y-1 cursor-pointer">
                <sound.icon className="w-8 h-8 text-primary mb-4 opacity-80 group-hover:opacity-100 transition-opacity" />
                <h3 className="font-medium text-foreground text-center mb-1">{sound.name}</h3>
                <span className="text-xs font-medium text-muted-foreground uppercase tracking-widest">{sound.duration}</span>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
