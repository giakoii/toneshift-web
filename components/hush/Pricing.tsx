"use client";
import { ScrollReveal } from "./ScrollReveal";
import { clsx } from "clsx";

export function Pricing() {
  const plans = [
    {
      name: "Free",
      price: "$0",
      desc: "Four sounds · five stories · timers up to 30 minutes",
      premium: false
    },
    {
      name: "Premium",
      price: "$4.99/mo",
      subprice: "or $29/year",
      desc: "All sounds and stories · offline downloads · unlimited timers · new story every week · seven days free",
      premium: true
    }
  ];

  return (
    <section className="py-24 md:py-32" id="pricing">
      <div className="mx-auto max-w-5xl px-6 md:px-8">
        <ScrollReveal>
          <div className="mb-16 md:mb-20 text-center">
            <h2 className="text-[clamp(32px,4vw,56px)] font-bold tracking-[-0.03em]">Free covers most nights.</h2>
          </div>
        </ScrollReveal>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
          {plans.map((plan, i) => (
            <ScrollReveal key={i} delay={i * 0.1}>
              <div className={clsx(
                "p-8 md:p-12 rounded-3xl flex flex-col h-full bg-card",
                plan.premium ? "border-2 border-primary relative" : "border border-border"
              )}>
                {plan.premium && (
                  <div className="absolute -top-3.5 left-1/2 -translate-x-1/2 bg-primary text-primary-foreground text-xs font-bold uppercase tracking-widest py-1 px-4 rounded-full">
                    Recommended
                  </div>
                )}
                
                <h3 className="text-2xl font-bold mb-2">{plan.name}</h3>
                <div className="flex items-baseline gap-2 mb-6">
                  <span className="text-4xl font-bold">{plan.price}</span>
                  {plan.subprice && <span className="text-muted-foreground">{plan.subprice}</span>}
                </div>
                
                <p className="text-muted-foreground font-light leading-[1.7] text-lg mb-10 flex-1">
                  {plan.desc}
                </p>
                
                <button className={clsx(
                  "w-full py-4 rounded-full font-semibold transition-all",
                  plan.premium ? "bg-primary text-primary-foreground hover:brightness-110" : "bg-foreground text-background hover:bg-foreground/90"
                )}>
                  {plan.premium ? "Start 7-day free trial" : "Download Free"}
                </button>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  );
}
