// 12 notes in the Western chromatic scale
export const CHROMATIC_SCALE = [
  "C", "C#", "D", "D#", "E", "F",
  "F#", "G", "G#", "A", "A#", "B"
];

// Flat aliases mapped to their sharp equivalents
export const FLAT_ALIASES: Record<string, string> = {
  "Db": "C#", "Eb": "D#", "Gb": "F#",
  "Ab": "G#", "Bb": "A#"
};

// Human-readable labels for dropdowns
export const KEY_DISPLAY_NAMES = [
  "C", "C# / Db", "D", "D# / Eb", "E", "F",
  "F# / Gb", "G", "G# / Ab", "A", "A# / Bb", "B"
];
