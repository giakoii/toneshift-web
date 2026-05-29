import type { ToneKey } from "@/types";
import { NOTE_NAMES } from "@/lib/parser/noteParser";
import { TONES } from "@/constants/tones";

/**
 * Detect the most likely original tone of a notation string.
 * Very simple heuristic: count occurrences of each scale note,
 * then pick the key whose diatonic set covers the most note types.
 *
 * TODO: replace with a more robust Bayesian / frequency-based approach
 * when connecting to the AI API.
 */

/** Diatonic (7-note) sets for each key (semitone positions) */
const DIATONIC_SETS: Record<ToneKey, number[]> = {
  C:  [0, 2, 4, 5, 7, 9, 11],
  D:  [2, 4, 6, 7, 9, 11, 1],
  E:  [4, 6, 8, 9, 11, 1, 3],
  F:  [5, 7, 9, 10, 0, 2, 4],
  G:  [7, 9, 11, 0, 2, 4, 6],
  A:  [9, 11, 1, 2, 4, 6, 8],
  B:  [11, 1, 3, 4, 6, 8, 10],
  "C#": [1, 3, 5, 6, 8, 10, 0],
  "D#": [3, 5, 7, 8, 10, 0, 2],
  "F#": [6, 8, 10, 11, 1, 3, 5],
  "G#": [8, 10, 0, 1, 3, 5, 7],
  "A#": [10, 0, 2, 3, 5, 7, 9],
  Db:  [1, 3, 5, 6, 8, 10, 0],
  Eb:  [3, 5, 7, 8, 10, 0, 2],
  Gb:  [6, 8, 10, 11, 1, 3, 5],
  Ab:  [8, 10, 0, 1, 3, 5, 7],
  Bb:  [10, 0, 2, 3, 5, 7, 9],
};

const NOTE_SEMITONES: Record<string, number> = {
  Do: 0, Reb: 1, Re: 2, Mib: 3, Mi: 4, Fa: 5,
  Fab: 6, Sol: 7, Lab: 8, La: 9, Sib: 10, Si: 11,
};

export function detectTone(notation: string): ToneKey | null {
  // Find all note names in text
  const noteRegex = new RegExp(`(${NOTE_NAMES.join("|")})`, "gi");
  const matches = notation.match(noteRegex);
  if (!matches || matches.length === 0) return null;

  // Count semitone frequencies
  const freq: Record<number, number> = {};
  for (const m of matches) {
    const key = m.charAt(0).toUpperCase() + m.slice(1).toLowerCase();
    const semitone = NOTE_SEMITONES[key];
    if (semitone !== undefined) freq[semitone] = (freq[semitone] ?? 0) + 1;
  }

  // Score each tone
  let bestTone: ToneKey = "C";
  let bestScore = -1;

  for (const tone of TONES) {
    const diatonic = DIATONIC_SETS[tone.key];
    let score = 0;
    for (const semitone of diatonic) {
      score += freq[semitone] ?? 0;
    }
    if (score > bestScore) {
      bestScore = score;
      bestTone = tone.key;
    }
  }

  return bestTone;
}
