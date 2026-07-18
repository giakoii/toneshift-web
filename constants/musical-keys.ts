// 12 notes in the Western chromatic scale
export const CHROMATIC_SCALE = [
  "C", "C#", "D", "D#", "E", "F",
  "F#", "G", "G#", "A", "A#", "B"
];

// Flat aliases mapped to their sharp equivalents
export const FLAT_ALIASES: Record<string, string> = {
  "Db": "C#", "Eb": "D#", "Fb": "E", "Gb": "F#",
  "Ab": "G#", "Bb": "A#", "Cb": "B"
};

// Human-readable labels for dropdowns (includes Vietnamese names)
export const KEY_DISPLAY_NAMES = [
  "C (Đô)", "C# / Db", "D (Rê)", "D# / Eb", "E (Mi)", "F (Fa)",
  "F# / Gb", "G (Sol)", "G# / Ab", "A (La)", "A# / Bb", "B (Si)"
];

// Vietnamese → Western root mapping
export const VIET_TO_WESTERN: Record<string, string> = {
  "Đô": "C", "Rê": "D", "Mi": "E", "Fa": "F",
  "Sol": "G", "La": "A", "Si": "B"
};

// Western → Vietnamese root mapping
export const WESTERN_TO_VIET: Record<string, string> = {
  "C": "Đô", "D": "Rê", "E": "Mi", "F": "Fa",
  "G": "Sol", "A": "La", "B": "Si"
};
