"use client";
import { useEffect, useState } from "react";
import { motion } from "framer-motion";

export function StarField() {
  const [stars, setStars] = useState<{ id: number; x: number; y: number; delay: number; duration: number }[]>([]);

  useEffect(() => {
    const newStars = Array.from({ length: 40 }).map((_, i) => ({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 100,
      delay: Math.random() * 5,
      duration: 3 + Math.random() * 2,
    }));
    setStars(newStars);
  }, []);

  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden>
      {stars.map((star) => (
        <motion.div
          key={star.id}
          className="absolute h-[1px] w-[1px] bg-foreground md:h-[2px] md:w-[2px] rounded-full"
          style={{ left: `${star.x}%`, top: `${star.y}%`, opacity: 0.1 }}
          animate={{ opacity: [0.1, 0.3, 0.1] }}
          transition={{
            duration: star.duration,
            repeat: Infinity,
            delay: star.delay,
            ease: "easeInOut",
          }}
        />
      ))}
    </div>
  );
}
