"use client";

import { motion, useReducedMotion } from "framer-motion";

export function AuroraBackground() {
  const reduce = useReducedMotion();

  if (reduce) {
    return (
      <div className="fixed inset-0 z-[-2] bg-background">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_30%,var(--background)_75%)]" />
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[-2] overflow-hidden bg-background pointer-events-none">
      {/* Blob 1 */}
      <motion.div
        animate={{
          rotate: [0, 360],
          scale: [1, 1.1, 1],
        }}
        transition={{
          rotate: { duration: 25, repeat: Infinity, ease: "linear" },
          scale: { duration: 15, repeat: Infinity, ease: "easeInOut" },
        }}
        className="absolute -top-[20%] -left-[10%] w-[50vw] h-[50vw] rounded-full bg-amber-600/30 blur-[120px] mix-blend-screen"
      />

      {/* Blob 2 */}
      <motion.div
        animate={{
          rotate: [360, 0],
          scale: [1, 1.2, 1],
        }}
        transition={{
          rotate: { duration: 30, repeat: Infinity, ease: "linear" },
          scale: { duration: 20, repeat: Infinity, ease: "easeInOut" },
        }}
        className="absolute top-[30%] -right-[10%] w-[60vw] h-[60vw] rounded-full bg-amber-500/20 blur-[140px] mix-blend-screen"
      />

      {/* Blob 3 */}
      <motion.div
        animate={{
          rotate: [0, -360],
          scale: [1, 1.3, 1],
          x: ["0%", "10%", "-10%", "0%"],
          y: ["0%", "-10%", "10%", "0%"],
        }}
        transition={{
          rotate: { duration: 35, repeat: Infinity, ease: "linear" },
          scale: { duration: 25, repeat: Infinity, ease: "easeInOut" },
          x: { duration: 20, repeat: Infinity, ease: "easeInOut" },
          y: { duration: 22, repeat: Infinity, ease: "easeInOut" },
        }}
        className="absolute -bottom-[20%] left-[20%] w-[55vw] h-[55vw] rounded-full bg-orange-600/25 blur-[130px] mix-blend-screen"
      />

      {/* Vignette overlay */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_0%,var(--background)_90%)]" />
    </div>
  );
}
