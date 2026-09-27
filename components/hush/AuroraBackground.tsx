"use client";
import { motion } from "framer-motion";

export function AuroraBackground() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden>
      <motion.div
        className="absolute -top-40 left-1/4 h-[320px] w-[320px] md:h-[560px] md:w-[560px] rounded-full bg-[hsl(258,78%,70%)] blur-[80px] md:blur-[120px] opacity-35"
        animate={{ x: [0, 160, 0], y: [0, 60, 0] }}
        transition={{ duration: 22, repeat: Infinity, ease: "easeInOut" }}
      />
      <motion.div
        className="absolute top-20 right-1/4 h-[320px] w-[320px] md:h-[560px] md:w-[560px] rounded-full bg-[hsl(178,62%,60%)] blur-[80px] md:blur-[120px] opacity-20"
        animate={{ x: [0, -160, 0], y: [0, -60, 0] }}
        transition={{ duration: 22, repeat: Infinity, ease: "easeInOut" }}
      />
    </div>
  );
}
