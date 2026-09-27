"use client";

import { motion, useReducedMotion } from "framer-motion";
import { useMousePosition } from "@/hooks/useMousePosition";
import { useEffect, useState } from "react";

export function MouseSpotlight() {
  const { x, y } = useMousePosition();
  const reduce = useReducedMotion();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (reduce || !mounted) return null;

  return (
    <motion.div
      className="pointer-events-none fixed inset-0 z-[-1] transition-opacity duration-300"
      animate={{
        background: `radial-gradient(600px circle at ${x}px ${y}px, rgba(255, 255, 255, 0.05), transparent 40%)`,
      }}
      transition={{ type: "tween", ease: "linear", duration: 0 }}
    />
  );
}
