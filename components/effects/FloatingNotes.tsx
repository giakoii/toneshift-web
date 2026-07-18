"use client";

import { motion, useReducedMotion } from "framer-motion";
import { Music, Music2, Music3, Music4 } from "lucide-react";
import { useEffect, useState } from "react";

const ICON_TYPES = [Music, Music2, Music3, Music4];

export function FloatingNotes() {
  const reduce = useReducedMotion();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (reduce || !mounted) return null;

  // Generate a fixed set of notes to avoid hydration mismatch
  const notes = [
    { id: 1, Icon: ICON_TYPES[0], top: "15%", left: "10%", delay: 0, duration: 4, size: 24 },
    { id: 2, Icon: ICON_TYPES[1], top: "60%", left: "5%", delay: 1, duration: 5, size: 32 },
    { id: 3, Icon: ICON_TYPES[2], top: "20%", left: "85%", delay: 2, duration: 4.5, size: 28 },
    { id: 4, Icon: ICON_TYPES[3], top: "75%", left: "80%", delay: 1.5, duration: 6, size: 36 },
    { id: 5, Icon: ICON_TYPES[0], top: "40%", left: "15%", delay: 0.5, duration: 5.5, size: 20 },
    { id: 6, Icon: ICON_TYPES[1], top: "30%", left: "75%", delay: 2.5, duration: 4.8, size: 24 },
  ];

  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none z-0">
      {notes.map((note) => (
        <motion.div
          key={note.id}
          className="absolute text-amber-500/20"
          style={{ top: note.top, left: note.left }}
          animate={{
            y: [0, -30, 0],
            rotate: [0, 10, -10, 0],
          }}
          transition={{
            y: {
              duration: note.duration,
              repeat: Infinity,
              ease: "easeInOut",
              delay: note.delay,
            },
            rotate: {
              duration: note.duration * 1.5,
              repeat: Infinity,
              ease: "easeInOut",
              delay: note.delay,
            },
          }}
        >
          <note.Icon size={note.size} />
        </motion.div>
      ))}
    </div>
  );
}
