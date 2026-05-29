import type { Tone, ToneKey } from "@/types";

/**
 * All supported tones with Vietnamese labels.
 * Semitone: C=0, C#=1, D=2, D#=3, E=4, F=5, F#=6, G=7, G#=8, A=9, A#=10, B=11
 */
export const TONES: Tone[] = [
  { key: "C",  label: "Đô (C)",   shortLabel: "C",  semitone: 0  },
  { key: "C#", label: "Đô# (C#)", shortLabel: "C#", semitone: 1  },
  { key: "D",  label: "Rê (D)",   shortLabel: "D",  semitone: 2  },
  { key: "D#", label: "Rê# (D#)", shortLabel: "D#", semitone: 3  },
  { key: "E",  label: "Mi (E)",   shortLabel: "E",  semitone: 4  },
  { key: "F",  label: "Fa (F)",   shortLabel: "F",  semitone: 5  },
  { key: "F#", label: "Fa# (F#)", shortLabel: "F#", semitone: 6  },
  { key: "G",  label: "Sol (G)",  shortLabel: "G",  semitone: 7  },
  { key: "G#", label: "Sol#(G#)", shortLabel: "G#", semitone: 8  },
  { key: "A",  label: "La (A)",   shortLabel: "A",  semitone: 9  },
  { key: "A#", label: "La# (A#)", shortLabel: "A#", semitone: 10 },
  { key: "B",  label: "Si (B)",   shortLabel: "B",  semitone: 11 },
];

export const TONES_MAP: Record<ToneKey, Tone> = Object.fromEntries(
  TONES.map((t) => [t.key, t])
) as Record<ToneKey, Tone>;

/** Get tone object by key */
export function getTone(key: ToneKey): Tone {
  return TONES_MAP[key];
}

/** Get semitone interval between two tones (positive = up) */
export function getInterval(from: ToneKey, to: ToneKey): number {
  const diff = getTone(to).semitone - getTone(from).semitone;
  return ((diff % 12) + 12) % 12; // always 0–11
}
